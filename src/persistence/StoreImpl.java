package persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import persistence.PersistentObject.MethodCall;
import persistence.server.DatabeansPrincipal;
import persistence.storage.Collector;
import persistence.storage.FileHeap;
import persistence.storage.Heap;
import persistence.storage.MemoryModel;

public class StoreImpl extends UnicastRemoteObject implements Collector, Store {
	final Heap heap;
	Map users;
	Map classes;
	Collection transactions;
	PersistentSystem system;
	final Subject systemSubject;
	final Connection systemConnection;
	final Map connections=new WeakHashMap();
	final Map cache=new WeakHashMap();
	boolean readOnly;
	boolean closing;
	long boot;

	public StoreImpl(String name) throws RemoteException {
		try {
			heap=new FileHeap(name,this);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		systemSubject=new Subject();
		systemSubject.getPrincipals().add(new DatabeansPrincipal("system"));
		systemConnection=new SystemConnection(this,systemSubject);
		if((boot=heap.boot())==0) {
			heap.mount(true);
			create();
			init();
		} else {
			if(heap.mounted()) {
				System.out.println("store was not cleanly unmounted, running in recovery mode");
				readOnly=true;
			} else heap.mount(true);
			instantiate();
			init();
			clear();
			gc(false);
			updateClasses();
		}
	}

	void clear() {
		if(readOnly) return;
		for(Iterator it=transactions.iterator();it.hasNext();it.remove()) {
			rollback(((Transaction)it.next()));
		}
	}

	void updateClasses() {
		if(readOnly) return;
		for(Iterator it=classes.values().iterator();it.hasNext();) {
			if(refCount(((PersistentClass)it.next()).base)==1) it.remove();
		}
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
		users.put("admin",crypt(""));
	}

	void createSystem() {
		system=(PersistentSystem)systemConnection.create(PersistentSystem.class);
		incRefCount(boot=system.accessor().base.longValue());
		heap.setBoot(boot);
	}

	void instantiate() {
		system=(PersistentSystem)instantiate(new Long(boot)).object();
	}

	void init() {
		users=system.getUsers();
		classes=system.getClasses();
		transactions=system.getTransactions();
	}

	MethodCall attach(MethodCall call) {
		return attach(call.target()).new MethodCall(call.method,call.types,attach(call.args));
	}

	Object attach(Object obj) {
		return obj instanceof PersistentObject?attach((PersistentObject)obj):obj;
	}

	Object[] attach(Object obj[]) {
		Object a[]=new Object[obj.length];
		for(int i=0;i<obj.length;i++) a[i]=attach(obj[i]);
		return a;
	}

	PersistentObject attach(PersistentObject obj) {
		if(!equals(obj.store())) throw new PersistentException("not the same store");
		return get(obj.base()).object();
	}

	AccessorImpl create(PersistentClass clazz) {
		byte b[]=new byte[clazz.size];
		long base=heap.alloc(b.length);
		heap.writeBytes(base,b);
		setClass(base,clazz=cache(clazz));
		return cache(AccessorImpl.newInstance(new Long(base),clazz,this));
	}

	PersistentClass cache(PersistentClass clazz) {
		synchronized(classes) {
			PersistentClass c=(PersistentClass)classes.get(clazz.name);
			if(c==null) classes.put(clazz.name,clazz);
			else clazz.base=c.base;
			return clazz;
		}
	}

	AccessorImpl cache(AccessorImpl obj) {
		synchronized(cache) {
			Long base=obj.base;
			hold(base.longValue());
			cache.put(base,new WeakReference(obj));
			return obj;
		}
	}

	AccessorImpl get(Long base) {
		Reference w=(Reference)cache.get(base);
		return w==null?null:(AccessorImpl)w.get();
	}

	AccessorImpl instantiate(Long base) {
		synchronized(cache) {
			AccessorImpl obj=get(base);
			if(obj==null) obj=cache(AccessorImpl.newInstance(base,getClass(base.longValue()),this));
			return obj;
		}
	}

	void release(AccessorImpl accessor) {
		synchronized(cache) {
			Long base=accessor.base;
			AccessorImpl obj=get(base);
			if(obj==null) release(base.longValue());
			else if(obj==accessor) {
				cache.remove(base);
				release(base.longValue());
			}
		}
	}

	static char salt(byte pw[]) {
		return (char)((pw[0] << 8) | pw[1]);
	}

	static byte[] crypt(String password) {
		Random r=new SecureRandom();
		byte b[]=new byte[2];
		r.nextBytes(b);
		return crypt(password,(char)((b[0] << 8) | b[1]));
	}

	static byte[] crypt(String password, char salt) {
		byte b[]=password.getBytes();
		byte a[]=new byte[2+b.length];
		a[0]=(byte)(salt >> 8);
		a[1]=(byte)(salt & 0xff);
		System.arraycopy(b,0,a,2,b.length);
		try {
			b=MessageDigest.getInstance("MD5").digest(a);
			byte c[]=new byte[2+b.length];
			System.arraycopy(a,0,c,0,2);
			System.arraycopy(b,0,c,2,b.length);
			return c;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	void changePassword(String username, String oldPassword, String newPassword) {
		if(closing) throw new PersistentException("store closing");
		if(oldPassword==null) AccessController.checkPermission(new AdminPermission("changePassword"));
		synchronized(users) {
			byte pw[]=(byte[])users.get(username);
			if(pw==null) throw new PersistentException("the user "+username+" doesn't exist");
			else {
				if(oldPassword==null || Arrays.equals(pw,crypt(oldPassword,salt(pw)))) users.put(username,crypt(newPassword));
				else throw new PersistentException("old password doesn't match");
			}
		}
	}

	void addUser(String username, String password) {
		if(closing) throw new PersistentException("store closing");
		AccessController.checkPermission(new AdminPermission("addUser"));
		synchronized(users) {
			if(users.containsKey(username)) throw new PersistentException("the user "+username+" already exists");
			else users.put(username,crypt(password));
		}
	}

	void deleteUser(String username) {
		if(closing) throw new PersistentException("store closing");
		AccessController.checkPermission(new AdminPermission("deleteUser"));
		synchronized(users) {
			if(!users.containsKey(username)) throw new PersistentException("the user "+username+" doesn't exist");
			else users.remove(username);
		}
	}

	void checkedClose() throws RemoteException {
		if(closing) throw new PersistentException("store closing");
		AccessController.checkPermission(new AdminPermission("close"));
		close();
	}

	void checkedGc() {
		if(closing) throw new PersistentException("store closing");
		AccessController.checkPermission(new AdminPermission("gc"));
		gc();
	}

	long allocatedSpace() {
		return heap.allocatedSpace();
	}

	long maxSpace() {
		return heap.maxSpace();
	}

	public boolean authenticate(String username, char[] password) {
		if(closing) throw new PersistentException("store closing");
		byte pw[]=(byte[])users.get(username);
		return pw==null?false:Arrays.equals(pw,crypt(new String(password),salt(pw)));
	}

	public Connection getConnection(CallbackHandler handler) throws RemoteException {
		if(closing) throw new PersistentException("store closing");
		if(readOnly) throw new PersistentException("store in recovery mode");
		return new Connection(this,Connection.TRANSACTION_READ_UNCOMMITTED,login(handler).getSubject());
	}

	public AdminConnection getAdminConnection(CallbackHandler handler) throws RemoteException {
		if(closing) throw new PersistentException("store closing");
		return new AdminConnection(this,readOnly,login(handler).getSubject());
	}

	static LoginContext login(CallbackHandler handler) {

		// Obtain a LoginContext, needed for authentication. Tell it
		// to use the LoginModule implementation specified by the
		// entry named "Sample" in the JAAS login configuration
		// file and to also use the specified CallbackHandler.
		LoginContext lc = null;
		try {
			lc = new LoginContext("Databeans", handler);
		} catch (LoginException le) {
			System.err.println("Cannot create LoginContext. "
				+ le.getMessage());
			System.exit(-1);
		} catch (SecurityException se) {
			System.err.println("Cannot create LoginContext. "
				+ se.getMessage());
			System.exit(-1);
		}

		// the user has 3 attempts to authenticate successfully
		int i;
		for (i = 0; i < 3; i++) {
			try {

				// attempt authentication
				lc.login();

				// if we return with no exception, authentication succeeded
				break;

			} catch (LoginException le) {

				System.err.println("Authentication failed:");
				System.err.println("  " + le.getMessage());
				try {
					Thread.currentThread().sleep(3000);
				} catch (Exception e) {
					// ignore
				}

			}
		}

		// did they fail three times?
		if (i == 3) {
			System.out.println("Sorry");
			throw new PersistentException("permission denied");
		}

		System.out.println("Authentication succeeded!");
		return lc;
	}

	Transaction getTransaction(String client) {
		Transaction trans=(Transaction)systemConnection.create(Transaction.class,new Class[] {String.class},new Object[] {client});
		transactions.add(trans);
		return trans;
	}

	void release(Transaction transaction) {
		transactions.remove(transaction);
		rollback(transaction);
	}

	void rollback(final Transaction transaction) {
		Subject.doAsPrivileged(systemSubject,new PrivilegedAction() {
			public Object run() {
				transaction.rollback();
				return null;
			}
		},null);
	}

	public void close() throws RemoteException {
		closing=true;
		UnicastRemoteObject.unexportObject(this,true);
		while(true) {
			try {
				for(Iterator it=connections.keySet().iterator();it.hasNext();it.remove()) {
					((RemoteConnectionImpl)it.next()).close();
				}
				break;
			} catch (ConcurrentModificationException e) {}
		}
		((RemoteConnectionImpl)systemConnection.connection).close();
		while(true) {
			try {
				for(Iterator it=cache.keySet().iterator();it.hasNext();it.remove()) {
					AccessorImpl obj=get((Long)it.next());
					if(obj!=null) obj.close();
				}
				break;
			} catch (ConcurrentModificationException e) {}
		}
		System.gc();
		heap.mount(false);
	}

	public void gc() {
		System.gc();
		gc(true);
	}

	void gc(boolean keep) {
		if(readOnly) return;
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
		return ptr==0?null:getClass(ptr)==null?readObject(ptr):instantiate(new Long(ptr));
	}

	void setReference(long base, Field field, Object value) {
		synchronized(heap) {
			long src=((Long)field.get(heap,base)).longValue();
			long dst=value==null?0:value instanceof AccessorImpl?((AccessorImpl)value).base.longValue():writeObject(value);
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
			PersistentClass clazz=(PersistentClass)readObject(ptr);
			if(clazz instanceof ArrayClass) {
				ArrayClass c=(ArrayClass)clazz;
				c.init(((Character)c.getField("typeCode").get(heap,base)).charValue(),((Integer)c.getField("length").get(heap,base)).intValue());
			}
			return clazz;
		}
	}

	void setClass(long base, PersistentClass clazz) {
		long ptr=writeObject(clazz);
		incRefCount(ptr);
		Field.CLASS.set(heap,base,new Long(ptr));
		if(clazz instanceof ArrayClass) {
			ArrayClass c=(ArrayClass)clazz;
			c.getField("typeCode").set(heap,base,new Character(c.typeCode));
			c.getField("length").set(heap,base,new Integer(c.length));
		}
	}

	AccessorImpl getLock(long base) {
		long ptr=((Long)Field.LOCK.get(heap,base)).longValue();
		return ptr==0?null:instantiate(new Long(ptr));
	}

	void setLock(long base, AccessorImpl transaction) {
		Field.LOCK.set(heap,base,transaction==null?new Long(0):transaction.base);
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
