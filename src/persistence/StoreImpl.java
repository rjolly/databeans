package persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import persistence.beans.XMLDecoder;
import persistence.beans.XMLEncoder;
import persistence.storage.Collector;
import persistence.storage.FileHeap;
import persistence.storage.Heap;
import persistence.storage.MemoryModel;

public class StoreImpl extends UnicastRemoteObject implements Collector, Store {
	static final User none=new User("none","");
	static final User anonymous=new User("anonymous","");
	final Heap heap;
	Map users;
	Map classes;
	Collection transactions;
	PersistentSystem system;
	final ConnectionImpl systemConnection;
	final Collection connections=Collections.synchronizedCollection(new ArrayList());
	final Map cache=new WeakHashMap();
	boolean closing;
	long boot;

	public StoreImpl(String name) throws RemoteException {
		try {
			heap=new FileHeap(name,this);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		systemConnection=new SystemConnection(this,none);
		if((boot=heap.boot())==0) {
			heap.mount(true);
			create();
			init();
		} else {
			if(heap.mounted()) {
				systemConnection.close();
				throw new PersistentException("heap not cleanly unmounted");
			} else heap.mount(true);
			instantiate();
			init();
			clear();
			gc(false);
			updateClasses();
		}
	}

	void clear() {
		for(Iterator it=transactions.iterator();it.hasNext();it.remove()) ((Transaction)it.next()).rollback();
	}

	void updateClasses() {
		for(Iterator it=classes.values().iterator();it.hasNext();) if(refCount(((PersistentClass)it.next()).base)==1) it.remove();
	}

	void create() {
		classes=new HashMap();
		createSystem();
		Map map=new HashMap();
		map.putAll(classes);
		classes.clear();
		putAllClasses(map);
		putAllClasses(classes);
		createUsers();
	}

	void putAllClasses(Map map) {
		Map classes=system.getClasses();
		for(Iterator it=map.entrySet().iterator();it.hasNext();) {
			Map.Entry e=(Map.Entry)it.next();
			classes.put(e.getKey(),e.getValue());
		}
	}

	void createUsers() {
		Map users=system.getUsers();
		users.put(none.getName(),none);
		users.put(anonymous.getName(),anonymous);
	}

	void createSystem() {
		system=(PersistentSystem)systemConnection.create(PersistentSystem.class);
		incRefCount(boot=system.base);
		heap.setBoot(boot);
	}

	void instantiate() {
		system=(PersistentSystem)systemConnection.attach(instantiate(boot));
	}

	void init() {
		users=system.getUsers();
		classes=system.getClasses();
		transactions=system.getTransactions();
	}

	PersistentSystem getSystem() {
		return system;
	}

	PersistentSystem getSystem(ConnectionImpl connection) {
		return (PersistentSystem)connection.attach(system);
	}

	Transaction getTransaction(String client) {
		return (Transaction)systemConnection.create(Transaction.class,new Class[] {String.class},new Object[] {client});
	}

	Accessor create(PersistentClass c) {
		byte b[]=new byte[c.size];
		long base=heap.alloc(b.length);
		heap.writeBytes(base,b);
		setClass(base,c=cache(c));
		return cache(Accessor.create(new Long(base),c,this));
	}

	PersistentClass cache(PersistentClass c) {
		synchronized(classes) {
			PersistentClass d;
			if((d=(PersistentClass)classes.get(c.name))==null) {
				classes.put(c.name,c);
			} else c.base=d.base;
			return c;
		}
	}

	Accessor cache(Accessor accessor) {
		synchronized(cache) {
			Long b=accessor.base;
			hold(b.longValue());
			cache.put(b,new WeakReference(accessor));
			return accessor;
		}
	}

	Accessor instantiate(long base) {
		synchronized(cache) {
			Long b=new Long(base);
			Accessor obj;
			Reference w;
			if((obj=(w=(Reference)cache.get(b))==null?null:(Accessor)w.get())==null) {
				obj=Accessor.create(b,getClass(base),this);
				hold(b.longValue());
				cache.put(b,new WeakReference(obj));
			};
			return obj;
		}
	}

	void release(Accessor accessor) {
		synchronized(cache) {
			Long b=accessor.base;
			Accessor obj;
			Reference w;
			if((obj=(w=(Reference)cache.get(b))==null?null:(Accessor)w.get())==null) {
				release(b.longValue());
			} else {
				if(obj==accessor) {
					cache.remove(b);
					release(b.longValue());
				}
			}
		}
	}

	public synchronized void createUser(String username, byte[] password) {
		if(closing) throw new PersistentException("store closing");
		synchronized(users) {
			if(users.containsKey(username)) throw new PersistentException("the user "+username+" already exists");
			else users.put(username, new User(username,password));
		}
	}

	public synchronized Connection getConnection(String username, byte[] password) throws RemoteException {
		if(closing) throw new PersistentException("store closing");
		User s=(User)users.get(username);
		if(s!=null && !s.equals(none) && Arrays.equals(s.password,password)) return new ConnectionImpl(this,Connection.TRANSACTION_READ_UNCOMMITTED,s);
		else throw new PersistentException("permission denied");
	}

	public synchronized char salt(String username) {
		if(closing) throw new PersistentException("store closing");
		User s=(User)users.get(username);
		return s==null?0:(char)((s.password[0] << 8) | s.password[1]);
	}

	public synchronized void inport(String name) {
		if(closing) throw new PersistentException("store closing");
		try {
			XMLDecoder d = new XMLDecoder(systemConnection,new BufferedInputStream(new FileInputStream(name)));
			system.setRoot(d.readObject());
			d.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void export(String name) {
		if(closing) throw new PersistentException("store closing");
		try {
			XMLEncoder e = new XMLEncoder(systemConnection,new BufferedOutputStream(new FileOutputStream(name)));
			e.writeObject(system.getRoot());
			e.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void close() {
		if(closing) throw new PersistentException("store closing");
		closing=true;
		synchronized(connections) {
			for(Iterator it=connections.iterator();it.hasNext();) ((ConnectionImpl)it.next()).kick();
			for(Iterator it=new ArrayList(connections).iterator();it.hasNext();) ((ConnectionImpl)it.next()).close(true);
		}
		systemConnection.close();
		heap.mount(false);
	}

	public synchronized void gc() {
		if(closing) throw new PersistentException("store closing");
		System.runFinalization();
		gc(true);
	}

	void gc(boolean keep) {
		synchronized(heap) {
			mark(boot);
			if(keep) mark();
			sweep();
		}
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

	void hold(long base) {
		synchronized(heap) {
			long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
			long s=r&MemoryModel.model.pointerMinValue;
			r=r^s;
			s=MemoryModel.model.pointerMinValue;
			Field.REF_COUNT.set(heap,base,new Long(r^s));
		}
	}

	void release(long base) {
		synchronized(heap) {
			long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
			long s=r&MemoryModel.model.pointerMinValue;
			r=r^s;
			s=0;
			Field.REF_COUNT.set(heap,base,new Long(r^s));
			if(r==0) free(base);
		}
	}

	void incRefCount(long base) {
		synchronized(heap) {
			long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
			long s=r&MemoryModel.model.pointerMinValue;
			r=(r^s)+1;
			Field.REF_COUNT.set(heap,base,new Long(r^s));
		}
	}

	void decRefCount(long base) {
		synchronized(heap) {
			if(!heap.status(base)) return;
			long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
			long s=r&MemoryModel.model.pointerMinValue;
			r=(r^s)-1;
			Field.REF_COUNT.set(heap,base,new Long(r^s));
			if(r==0 && !inuse(base)) free(base);
		}
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

	void setReference(long base, Field field, Object value) {
		synchronized(heap) {
			long src=((Long)field.get(heap,base)).longValue();
			long dst=value==null?0:value instanceof Accessor?((Accessor)value).base.longValue():writeObject(value);
			if(dst!=0) incRefCount(dst);
			field.set(heap,base,new Long(dst));
			if(src!=0) decRefCount(src);
		}
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
		Field.LOCK.set(heap,base,accessor==null?new Long(0):accessor.base);
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

		public boolean mounted() {
			return false;
		}

		public void mount(boolean n) {
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
