package persistence;

import java.io.*;
import java.util.*;
import java.lang.ref.*;
import java.rmi.*;
import java.rmi.server.*;
import persistence.util.*;
import persistence.storage.*;
import persistence.beans.*;

public class StoreImpl extends UnicastRemoteObject implements Collector, Store {
	static final User none=new User("none","");
	static final User anonymous=new User("anonymous","");
	Heap heap;
	Map users;
	Map classes;
	Collection transactions;
	PersistentSystem system;
	ConnectionImpl systemConnection;
	Map cache=new WeakHashMap();
	Collection connections=Collections.synchronizedCollection(new ArrayList());
	boolean closed;
	long boot;

	public StoreImpl(String name) throws RemoteException {
		try {
			heap=new FileHeap(name,this);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		systemConnection=new ConnectionImpl(this,Connection.TRANSACTION_NONE,none);
		if((boot=heap.boot())==0) {
			heap.mount(true);
			createSystem();
			init();
			users.put(none.getName(), none);
			users.put(anonymous.getName(), anonymous);
		} else {
			if(!heap.mount(true)) {
				systemConnection.close();
				UnicastRemoteObject.unexportObject(this,true);
				throw new PersistentException("not cleanly unmounted");
			}
			instantiateSystem();
			init();
			clearTransactions();
			gc(false);
			updateClasses();
		}
	}

	void clearTransactions() {
		Iterator t=new ArrayList(transactions).iterator();
		while(t.hasNext()) ((Transaction)t.next()).rollback();
	}

	void updateClasses() {
		Iterator t=classes.values().iterator();
		while(t.hasNext()) {
			long ptr=((PersistentClass)t.next()).base;
			if(refCount(ptr)==1) t.remove();
		}
	}

	void createSystem() {
		classes=new HashMap();
		{
			system=(PersistentSystem)systemConnection.create(PersistentSystem.class);
			incRefCount(boot=system.accessor.base);
			heap.setBoot(boot);
			heap.mount(true);
		}
		Map map=new HashMap();
		map.putAll(classes);
		classes.clear();
		putAllClasses(map);
		putAllClasses(classes);
	}

	void instantiateSystem() {
		system=(PersistentSystem)systemConnection.attach(instantiate(boot));
	}

	void putAllClasses(Map map) {
		Map classes=PersistentCollections.localMap(system.getClasses());
		Iterator t=map.entrySet().iterator();
		while(t.hasNext()) {
			Map.Entry e=(Map.Entry)t.next();
			classes.put(e.getKey(),e.getValue());
		}
	}

	void init() {
		users=PersistentCollections.localMap(system.getUsers());
		classes=PersistentCollections.localMap(system.getClasses());
		transactions=PersistentCollections.localCollection(system.getTransactions());
	}

	Object attach(ConnectionImpl connection, Object obj) {
		return systemConnection.attach(connection,obj);
	}

	Object[] attach(ConnectionImpl connection, Object obj[]) {
		Object a[]=new Object[obj.length];
		for(int i=0;i<obj.length;i++) a[i]=attach(connection,obj[i]);
		return a;
	}

	public void createUser(String username, byte[] password) {
		if(closed) throw new PersistentException("store closed");
		synchronized(users) {
			if(users.containsKey(username)) throw new PersistentException("the user "+username+" already exists");
			else users.put(username, new User(username,password));
		}
	}

	public Connection getConnection(String username, byte[] password) throws RemoteException {
		if(closed) throw new PersistentException("store closed");
		User s=(User)users.get(username);
		if(s!=null && !s.equals(none) && Arrays.equals(s.password,password)) return new ConnectionImpl(this,Connection.TRANSACTION_READ_UNCOMMITTED,s);
		else throw new PersistentException("permission denied");
	}

	public char salt(String username) {
		if(closed) throw new PersistentException("store closed");
		User s=(User)users.get(username);
		return s==null?0:(char)((s.password[0] << 8) | s.password[1]);
	}

	Transaction getTransaction(String client) {
		return (Transaction)systemConnection.create(Transaction.class,new Class[] {String.class},new Object[] {client});
	}

	PersistentSystem getSystem(ConnectionImpl connection) {
		return (PersistentSystem)connection.attach(systemConnection,system);
	}

	PersistentSystem getSystem() {
		return system;
	}

	synchronized Accessor create(PersistentClass c) {
		byte b[]=new byte[c.size];
		long base=heap.alloc(b.length);
		heap.writeBytes(base,b);
		setClass(base,c=cache(c));
		return cache(new Accessor(base,c,this));
	}

	PersistentClass cache(PersistentClass c) {
		PersistentClass d;
		if((d=(PersistentClass)classes.get(c.name))==null) {
			classes.put(c.name,c);
		} else c.base=d.base;
		return c;
	}

	Accessor cache(Accessor obj) {
		hold(obj.base);
		cache.put(obj,new WeakReference(obj));
		return obj;
	}

	Accessor instantiate(long base) {
		return cache(base);
	}

	synchronized Accessor cache(long base) {
		Accessor b;
		Accessor obj=new Accessor(base,getClass(base),this);
		Reference w;
		if((b=(w=(Reference)cache.get(obj))==null?null:(Accessor)w.get())==null) {
			hold(obj.base);
			cache.put(obj,new WeakReference(obj));
		} else obj=b;
		return obj;
	}

	synchronized void release(Accessor obj) {
		Accessor b;
		Reference w;
		if((b=(w=(Reference)cache.get(obj))==null?null:(Accessor)w.get())==null) {
			release(obj.base);
		} else {
			if(b==obj) {
				cache.remove(obj);
				release(obj.base);
			}
		}
	}

	public void inport(String name) {
		if(closed) throw new PersistentException("store closed");
		try {
			XMLDecoder d = new XMLDecoder(systemConnection,new BufferedInputStream(new FileInputStream(name)));
			system.setRoot(d.readObject());
			d.close();
		} catch (IOException e) {
			throw new PersistentException("i/o error");
		}
	}

	public void export(String name) {
		if(closed) throw new PersistentException("store closed");
		try {
			XMLEncoder e = new XMLEncoder(systemConnection,new BufferedOutputStream(new FileOutputStream(name)));
			e.writeObject(system.getRoot());
			e.close();
		} catch (IOException e) {
			throw new PersistentException("i/o error");
		}
	}

	void disconnect() {
		Collection collection;
		synchronized(connections) {
			collection=new ArrayList(connections);
		}
		Iterator t=collection.iterator();
		while(t.hasNext()) {
			ConnectionImpl c=(ConnectionImpl)t.next();
			if(c!=systemConnection) c.close();
		}
	}

	public void close() {
		if(closed) throw new PersistentException("store closed");
		disconnect();
		systemConnection.close();
		heap.mount(false);
		closed=true;
	}

	public synchronized void gc() {
		if(closed) throw new PersistentException("store closed");
		System.runFinalization();
		gc(true);
	}

	void gc(boolean keep) {
		mark(boot);
		if(keep) mark();
		sweep();
	}

	void mark() {
		Iterator t=heap.iterator();
		while(t.hasNext()) {
			long ptr=((Long)t.next()).longValue();
			if(heap.status(ptr)) {
				 if(inuse(ptr)) mark(ptr);
			}
		}
	}

	void sweep() {
		Iterator t=heap.iterator();
		while(t.hasNext()) {
			long ptr=((Long)t.next()).longValue();
			if(heap.status(ptr)) {
				if(!heap.mark(ptr,false)) free(ptr);
			}
		}
	}

	void mark(long base) {
		if(heap.mark(base,true)) return;
		PersistentClass c=getClass(base);
		if(c!=null) {
			mark(c.base);
			Iterator t=c.fieldIterator();
			while(t.hasNext()) {
				Field field=(Field)t.next();
				switch (field.typeCode) {
				case 'Z':
				case 'B':
				case 'C':
				case 'S':
				case 'I':
				case 'F':
				case 'J':
				case 'D':
					break;
				case '[':
				case 'L':
					long ptr=((Long)field.get(heap,base)).longValue();
					if(ptr!=0) mark(ptr);
					break;
				default:
					throw new PersistentException("internal error");
				}
			}
		}
	}

	void free(long base) {
		if(!heap.status(base)) return;
		Heap patch=patch(base);
		heap.free(base);
		PersistentClass c=getClass(patch,base);
		if(c!=null) {
//			Field.CLASS.set(patch,base,new Long(0));
			decRefCount(c.base);
			Iterator t=c.fieldIterator();
			while(t.hasNext()) {
				Field field=(Field)t.next();
				switch (field.typeCode) {
				case 'Z':
				case 'B':
				case 'C':
				case 'S':
				case 'I':
				case 'F':
				case 'J':
				case 'D':
					break;
				case '[':
				case 'L':
					long ptr=((Long)field.get(patch,base)).longValue();
//					field.set(patch,base,new Long(0));
					if(ptr!=0) decRefCount(ptr);
					break;
				default:
					throw new PersistentException("internal error");
				}
			}
		}
	}

	boolean inuse(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		return s==MemoryModel.model.pointerMinValue;
	}

	synchronized void hold(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=r^s;
		s=MemoryModel.model.pointerMinValue;
		Field.REF_COUNT.set(heap,base,new Long(r^s));
	}

	synchronized void release(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=r^s;
		s=0;
		Field.REF_COUNT.set(heap,base,new Long(r^s));
		if(r==0) free(base);
	}

	synchronized void incRefCount(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=(r^s)+1;
		Field.REF_COUNT.set(heap,base,new Long(r^s));
	}

	synchronized void decRefCount(long base) {
		if(!heap.status(base)) return;
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=(r^s)-1;
		Field.REF_COUNT.set(heap,base,new Long(r^s));
		if(r==0 && !inuse(base)) free(base);
	}

	long refCount(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		return r^s;
	}

	Object get(long base, Field field) {
		return field.reference?getReference(base,field):field.get(heap,base);
	}

	void set(long base, Field field, Object value) {
		if(field.reference) setReference(base,field,value);
		else field.set(heap,base,value);
	}

	Object getReference(long base, Field field) {
		long ptr=((Long)field.get(heap,base)).longValue();
		return ptr==0?null:getClass(ptr)==null?readObject(ptr):instantiate(ptr);
	}

	synchronized void setReference(long base, Field field, Object value) {
		long src=((Long)field.get(heap,base)).longValue();
		long dst=value==null?0:value instanceof Accessor?((Accessor)value).base:writeObject(value);
		if(dst!=0) incRefCount(dst);
		field.set(heap,base,new Long(dst));
		if(src!=0) decRefCount(src);
	}

	PersistentClass getClass(long base) {
		return getClass(heap,base);
	}

	PersistentClass getClass(Heap heap, long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		if(ptr==0) return null;
		else {
			PersistentClass c=(PersistentClass)readObject(ptr);
			if(c instanceof ArrayClass) ((ArrayClass)c).init(((Character)c.getField("typeCode").get(heap,base)).charValue(),((Integer)c.getField("length").get(heap,base)).intValue());
			return c;
		}
	}

	void setClass(long base, PersistentClass c) {
		long ptr=writeObject(c);
		incRefCount(ptr);
		Field.CLASS.set(heap,base,new Long(ptr));
		if(c instanceof ArrayClass) {
			c.getField("typeCode").set(heap,base,new Character(((ArrayClass)c).typeCode));
			c.getField("length").set(heap,base,new Integer(((ArrayClass)c).length));
		}
	}

	Accessor getLock(long base) {
		long ptr=((Long)Field.LOCK.get(heap,base)).longValue();
		return ptr==0?null:instantiate(ptr);
	}

	void setLock(long base, Accessor accessor) {
		Field.LOCK.set(heap,base,new Long(accessor==null?0:accessor.base));
	}

	Object readObject(long base) {
		return readObject(heap,base);
	}

	Object readObject(Heap heap, long base) {
		Object obj;
		byte b[]=heap.readBytes(base);
		InputStream is=new ByteArrayInputStream(b,Field.LOCK.offset,b.length-Field.LOCK.offset);
		try {
			obj=new ObjectInputStream(is).readObject();
		} catch (ClassNotFoundException e) {
			throw new PersistentException("class not found");
		} catch (IOException e) {
			throw new PersistentException("deserialization error");
		}
		if(obj instanceof UnicastSerializedObject) {
			UnicastSerializedObject u=(UnicastSerializedObject)obj;
			u.base=base;
		}
		return obj;
	}

	long writeObject(Object obj) {
		if(obj instanceof UnicastSerializedObject) {
			UnicastSerializedObject u=(UnicastSerializedObject)obj;
			if(u.base!=0) return u.base;
		}
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(os).writeObject(obj);
		} catch (IOException e) {
			throw new PersistentException("serialization error");
		}
		byte b[]=os.toByteArray();
		byte s[]=new byte[Field.LOCK.offset+b.length];
		System.arraycopy(b,0,s,s.length-b.length,b.length);
		long base=heap.alloc(s.length);
		heap.writeBytes(base,s);
		if(obj instanceof UnicastSerializedObject) {
			UnicastSerializedObject u=(UnicastSerializedObject)obj;
			u.base=base;
		}
		return base;
	}

	Heap patch(long ptr) {
		return new Patch(ptr);
	}

	class Patch implements Heap {
		DataInputStream is;
		byte cache[];
		long ptr;

		Patch(long ptr) {
			this.ptr=ptr;
			cache=heap.readBytes(ptr);
			is=new DataInputStream(new ByteArrayInputStream(cache));
		}

		public long boot() {
			return 0;
		}

		public void setBoot(long ptr) {
		}

		public boolean mount(boolean n) {
			return false;
		}

		public long alloc(int size) {
			return 0;
		}

		public long realloc(long ptr, int size) {
			return 0;
		}

		public void free(long ptr) {
		}

		public boolean mark(long ptr, boolean n) {
			return false;
		}

		public boolean status(long ptr) {
			return false;
		}

		public long allocatedSpace() {
			return 0;
		}

		public long maxSpace() {
			return 0;
		}

		public Iterator iterator() {
			return null;
		}

		public synchronized boolean readBoolean(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readBoolean();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public synchronized byte readByte(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readByte();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public synchronized short readShort(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readShort();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public synchronized char readChar(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readChar();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public synchronized int readInt(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readInt();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public synchronized long readLong(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readLong();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public synchronized float readFloat(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readFloat();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public synchronized double readDouble(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readDouble();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public byte[] readBytes(long ptr) {
			return cache;
		}

		public void writeBoolean(long ptr, boolean v) {}
		public void writeByte(long ptr, int v) {}
		public void writeShort(long ptr, int v) {}
		public void writeChar(long ptr, int v) {}
		public void writeInt(long ptr, int v) {}
		public void writeLong(long ptr, long v) {}
		public void writeFloat(long ptr, float v) {}
		public void writeDouble(long ptr, double v) {}
		public void writeBytes(long ptr, byte b[]) {}
	}
}
