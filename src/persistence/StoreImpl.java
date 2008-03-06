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
import java.security.AccessController;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import persistence.PersistentObject.MethodCall;
import persistence.beans.XMLDecoder;
import persistence.beans.XMLEncoder;
import persistence.server.Login;
import persistence.storage.Collector;
import persistence.storage.FileHeap;
import persistence.storage.Heap;
import persistence.storage.MemoryModel;

public class StoreImpl extends UnicastRemoteObject implements Collector, Store {
	final Heap heap;
	final Connection systemConnection;
	final Map connections=new WeakHashMap();
	final Map cache=new WeakHashMap();
	PersistentSystem system;
	boolean readOnly;
	boolean closed;
	Map classes;
	long boot;

	public StoreImpl(String name) throws RemoteException {
		try {
			heap=new FileHeap(name,this);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		systemConnection=new SystemConnection(this);
		if((boot=heap.boot())==0) {
			heap.mount(true);
			create();
		} else {
			if(heap.mounted()) {
				System.out.println("store was not cleanly unmounted, running in recovery mode");
				readOnly=true;
			} else heap.mount(true);
			instantiate();
		}
	}

	synchronized void create() {
		classes=new LinkedHashMap();
		createSystem();
		createUsers();
		system.getClasses().putAll(new LinkedHashMap(classes));
		system.getClasses().putAll(classes);
		classes=system.getClasses();
	}

	void createSystem() {
		system=(PersistentSystem)systemConnection.create(PersistentSystem.class);
		incRefCount(boot=system.base.longValue());
		heap.setBoot(boot);
	}

	void createUsers() {
		Map users=system.getUsers();
		users.put("admin",new Password(""));
	}

	synchronized void instantiate() {
		system=(PersistentSystem)instantiate(boot);
		classes=system.getClasses();
		if(readOnly) return;
		rollback();
		gc(false);
		updateClasses();
	}

	void rollback() {
		for(Iterator it=system.getTransactions().iterator();it.hasNext();it.remove()) {
			((Transaction)it.next()).rollback(null);
		}
	}

	void updateClasses() {
		for(Iterator it=classes.values().iterator();it.hasNext();) {
			if(refCount(((PersistentClass)it.next()).base.longValue())==1) it.remove();
		}
	}

	PersistentClass get(Class clazz) {
		String name=clazz.getName();
		synchronized(classes) {
			PersistentClass c=(PersistentClass)classes.get(name);
			if(c==null) classes.put(name,c=PersistentClass.create(clazz,this));
			return c;
		}
	}

	PersistentClass get(Class componentType, int length) {
		return ArrayClass.create(componentType,length,this);
	}

	synchronized PersistentObject create(PersistentClass clazz) {
		byte b[]=new byte[clazz.size()];
		long base=heap.alloc(b.length);
		heap.writeBytes(base,b);
		setClass(base,clazz);
		hold(base);
		synchronized(cache) {
			return cache(PersistentObject.newInstance(base,clazz,this));
		}
	}

	PersistentObject instantiate(long base) {
		hold(base);
		synchronized(cache) {
			PersistentObject o=get(new Long(base));
			return o==null?cache(selfClass(base)?PersistentClass.newInstance(base,this):PersistentObject.newInstance(base,getClass(base),this)):o;
		}
	}

	PersistentObject cache(PersistentObject obj) {
		cache.put(obj.base,new WeakReference(obj));
		return obj;
	}

	PersistentObject get(Long base) {
		Reference w=(Reference)cache.get(base);
		return w==null?null:(PersistentObject)w.get();
	}

	synchronized void release(PersistentObject obj) {
		if(closed) return;
		if(readOnly) return;
		PersistentObject o;
		synchronized(cache) {
			o=get(obj.base);
			if(o==obj) cache.remove(obj.base);
		}
		if(o==null || o==obj) release(obj.base.longValue());
	}

	MethodCall attach(MethodCall call) {
		return attach(call.target()).new MethodCall(call.method,call.types,attach(call.args));
	}

	Object attach(Object obj) {
		if(obj instanceof PersistentObject) return attach((PersistentObject)obj);
		if(obj instanceof Object[]) return attach((Object[])obj);
		return obj;
	}

	Object[] attach(Object obj[]) {
		for(int i=0;i<obj.length;i++) obj[i]=attach(obj[i]);
		return obj;
	}

	PersistentObject attach(PersistentObject obj) {
		if(!equals(obj.store())) throw new PersistentException("not the same store");
		synchronized(cache) {
			return get(obj.base());
		}
	}

	void changePassword(String username, String oldPassword, String newPassword) {
		if(oldPassword==null) AccessController.checkPermission(new AdminPermission("changePassword"));
		Map users=system.getUsers();
		synchronized(users) {
			Password pw=(Password)users.get(username);
			if(pw==null) throw new PersistentException("the user "+username+" doesn't exist");
			else {
				if(oldPassword==null || pw.match(oldPassword)) users.put(username,new Password(newPassword));
				else throw new PersistentException("old password doesn't match");
			}
		}
	}

	void addUser(String username, String password) {
		AccessController.checkPermission(new AdminPermission("addUser"));
		Map users=system.getUsers();
		synchronized(users) {
			if(users.containsKey(username)) throw new PersistentException("the user "+username+" already exists");
			else users.put(username,new Password(password));
		}
	}

	void deleteUser(String username) {
		AccessController.checkPermission(new AdminPermission("deleteUser"));
		Map users=system.getUsers();
		synchronized(users) {
			if(!users.containsKey(username)) throw new PersistentException("the user "+username+" doesn't exist");
			else users.remove(username);
		}
	}

	void inport(String name) {
		AccessController.checkPermission(new AdminPermission("import"));
		try {
			XMLDecoder d = new XMLDecoder(systemConnection,new BufferedInputStream(new FileInputStream(name)));
			system.setRoot(d.readObject());
			d.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void export(String name) {
		AccessController.checkPermission(new AdminPermission("export"));
		try {
			XMLEncoder e = new XMLEncoder(systemConnection,new BufferedOutputStream(new FileOutputStream(name)));
			e.writeObject(system.getRoot());
			e.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void shutdown() throws RemoteException {
		AccessController.checkPermission(new AdminPermission("shutdown"));
		close();
		System.gc();
	}

	void userGc() {
		AccessController.checkPermission(new AdminPermission("gc"));
		System.gc();
		gc();
	}

	long allocatedSpace() {
		return heap.allocatedSpace();
	}

	long maxSpace() {
		return heap.maxSpace();
	}

	public boolean authenticate(String username, char[] password) {
		Password pw=(Password)system.getUsers().get(username);
		return pw==null?false:pw.match(password);
	}

	public Connection getConnection(CallbackHandler handler) throws RemoteException {
		if(readOnly) throw new PersistentException("store in recovery mode");
		return new Connection(this,Connection.TRANSACTION_READ_UNCOMMITTED,Login.login(handler).getSubject());
	}

	public AdminConnection getAdminConnection(CallbackHandler handler) throws RemoteException {
		return new AdminConnection(this,readOnly,Login.login(handler).getSubject());
	}

	Transaction getTransaction(String client) {
		Transaction trans=(Transaction)systemConnection.create(Transaction.class,new Class[] {String.class},new Object[] {client});
		system.getTransactions().add(trans);
		return trans;
	}

	synchronized void release(Transaction transaction, Subject subject) {
		if(closed) return;
//		if(readOnly) return;
		system.getTransactions().remove(transaction);
		transaction.rollback(subject);
	}

	public synchronized void close() throws RemoteException {
		if(closed) return;
		UnicastRemoteObject.unexportObject(this,true);
		for(Iterator it=system.getTransactions().iterator();it.hasNext();) {
			((Transaction)it.next()).abort();
		}
		for(Iterator it=connections.keySet().iterator();it.hasNext();it.remove()) {
			((RemoteConnectionImpl)it.next()).close();
		}
		for(Iterator it=cache.keySet().iterator();it.hasNext();it.remove()) {
			close((Long)it.next());
		}
		((RemoteConnectionImpl)systemConnection.connection).close();
		systemConnection.close();
		if(!readOnly) heap.mount(false);
		closed=true;
	}

	void close(Long base) throws RemoteException {
		PersistentObject o;
		synchronized(cache) {
			o=get(base);
		}
		if(o!=null) {
			((PersistentObject.Accessor)o.accessor).close();
			o.close();
		}
	}

	public synchronized void gc() {
		if(closed) return;
		if(readOnly) return;
		gc(true);
	}

	void gc(boolean keep) {
		mark(keep);
		mark(boot);
		sweep();
	}

	void mark(boolean keep) {
		Iterator t=heap.iterator();
		while(t.hasNext()) {
			long ptr=((Long)t.next()).longValue();
			if(heap.status(ptr)) {
				markClass(ptr);
				if(keep && inuse(ptr)) mark(ptr);
			}
		}
	}

	void markClass(long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		if (ptr!=0) mark(ptr);
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
			mark(c.base.longValue());
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
			decRefCount(c.base.longValue());
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
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=r^s;
		s=MemoryModel.model.pointerMinValue;
		Field.REF_COUNT.set(heap,base,new Long(r|s));
	}

	void release(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=r^s;
		s=0;
		Field.REF_COUNT.set(heap,base,new Long(r|s));
		if(r==0) free(base);
	}

	void incRefCount(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=(r^s)+1;
		Field.REF_COUNT.set(heap,base,new Long(r|s));
	}

	void decRefCount(long base) {
		if(!heap.status(base)) return;
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		r=(r^s)-1;
		Field.REF_COUNT.set(heap,base,new Long(r|s));
		if(r==0 && !inuse(base)) free(base);
	}

	long refCount(long base) {
		long r=((Long)Field.REF_COUNT.get(heap,base)).longValue();
		long s=r&MemoryModel.model.pointerMinValue;
		return r^s;
	}

	synchronized Object get(long base, Field field) {
		return field.reference?getReference(base,field):field.get(heap,base);
	}

	synchronized void set(long base, Field field, Object value) {
		if(field.reference) setReference(base,field,value);
		else field.set(heap,base,value);
	}

	Object getReference(long base, Field field) {
		long ptr=((Long)field.get(heap,base)).longValue();
		return ptr==0?null:getClass(ptr)==null?readObject(ptr):instantiate(ptr);
	}

	void setReference(long base, Field field, Object value) {
		long src=((Long)field.get(heap,base)).longValue();
		long dst=value==null?0:value instanceof PersistentObject?((PersistentObject)value).base.longValue():writeObject(value);
		if(dst!=0) incRefCount(dst);
		field.set(heap,base,new Long(dst));
		if(src!=0) decRefCount(src);
	}

	PersistentClass getClass(long base) {
		return getClass(heap,base);
	}

	PersistentClass getClass(Heap heap, long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		return ptr==0?null:(PersistentClass)instantiate(ptr);
	}

	boolean selfClass(long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		return ptr==base;
	}

	void setClass(long base, PersistentClass clazz) {
		long ptr=clazz.base==null?base:clazz.base.longValue();
		incRefCount(ptr);
		Field.CLASS.set(heap,base,new Long(ptr));
	}

	synchronized Transaction getLock(long base) {
		long ptr=((Long)Field.LOCK.get(heap,base)).longValue();
		return ptr==0?null:(Transaction)instantiate(ptr);
	}

	synchronized void setLock(long base, Transaction transaction) {
		Field.LOCK.set(heap,base,new Long(transaction==null?0:transaction.base.longValue()));
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
		return obj;
	}

	long writeObject(Object obj) {
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

		public boolean readBoolean(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readBoolean();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public byte readByte(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readByte();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public short readShort(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readShort();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public char readChar(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readChar();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public int readInt(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readInt();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public long readLong(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readLong();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public float readFloat(long ptr) {
			try {
				is.reset();
				is.skip(ptr-this.ptr);
				return is.readFloat();
			} catch (IOException e) {
				throw new PersistentException("internal error");
			}
		}

		public double readDouble(long ptr) {
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
