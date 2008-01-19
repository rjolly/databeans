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
import java.security.SecureRandom;
import java.util.Arrays;
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

	void create() {
		classes=new HashMap();
		createSystem();
		createUsers();
		system.getClasses().putAll(new HashMap(classes));
		system.getClasses().putAll(classes);
		classes=system.getClasses();
	}

	void createSystem() {
		system=(PersistentSystem)systemConnection.create(PersistentSystem.class);
		incRefCount(boot=system.accessor().base.longValue());
		heap.setBoot(boot);
	}

	void createUsers() {
		Map users=system.getUsers();
		users.put("admin",crypt(""));
	}

	void instantiate() {
		system=(PersistentSystem)instantiate(boot).object();
		classes=system.getClasses();
		clear();
	}

	void clear() {
		if(readOnly) return;
		for(Iterator it=system.getTransactions().iterator();it.hasNext();it.remove()) {
			((Transaction)it.next()).rollback(null);
		}
		gc(false);
		for(Iterator it=classes.values().iterator();it.hasNext();) {
			if(refCount(((PersistentClass)it.next()).accessor().base.longValue())==1) it.remove();
		}
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
		return get(obj.base().longValue()).object();
	}

	PersistentClass get(Class clazz) {
		synchronized(classes) {
			String name=clazz.getName();
			PersistentClass c=(PersistentClass)classes.get(name);
			if(c==null) classes.put(name,c=PersistentClass.create(clazz,this));
			return c;
		}
	}

	PersistentClass get(Class componentType, int length) {
		synchronized(classes) {
			String name=ArrayClass.name(componentType,length);
			PersistentClass c=(PersistentClass)classes.get(name);
			if(c==null) classes.put(name,c=ArrayClass.create(componentType,length,this));
			return c;
		}
	}

	AccessorImpl create(PersistentClass clazz) {
		byte b[]=new byte[clazz.size()];
		long base=heap.alloc(b.length);
		heap.writeBytes(base,b);
		setClass(base,clazz.accessor());
		return cache(AccessorImpl.newInstance(base,clazz,this));
	}

	AccessorImpl cache(AccessorImpl obj) {
		synchronized(heap) {
			Long base=obj.base;
			hold(base.longValue());
			cache.put(base,new WeakReference(obj));
			return obj;
		}
	}

	AccessorImpl get(long base) {
		Reference w=(Reference)cache.get(new Long(base));
		return w==null?null:(AccessorImpl)w.get();
	}

	AccessorImpl instantiate(long base) {
		synchronized(heap) {
			AccessorImpl obj=get(base);
			if(obj==null) obj=cache(AccessorImpl.newInstance(base,(PersistentClass)getClass(base).object(),this));
			return obj;
		}
	}

	synchronized void release(AccessorImpl accessor) {
		if(readOnly) return;
		if(closed) return;
		Long base=accessor.base;
		AccessorImpl obj=get(base.longValue());
		if(obj==null) release(base.longValue());
		else if(obj==accessor) {
			cache.remove(base);
			release(base.longValue());
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

	synchronized void changePassword(String username, String oldPassword, String newPassword) {
		if(oldPassword==null) AccessController.checkPermission(new AdminPermission("changePassword"));
		Map users=system.getUsers();
		synchronized(users) {
			byte pw[]=(byte[])users.get(username);
			if(pw==null) throw new PersistentException("the user "+username+" doesn't exist");
			else {
				if(oldPassword==null || Arrays.equals(pw,crypt(oldPassword,salt(pw)))) users.put(username,crypt(newPassword));
				else throw new PersistentException("old password doesn't match");
			}
		}
	}

	synchronized void addUser(String username, String password) {
		AccessController.checkPermission(new AdminPermission("addUser"));
		Map users=system.getUsers();
		synchronized(users) {
			if(users.containsKey(username)) throw new PersistentException("the user "+username+" already exists");
			else users.put(username,crypt(password));
		}
	}

	synchronized void deleteUser(String username) {
		AccessController.checkPermission(new AdminPermission("deleteUser"));
		Map users=system.getUsers();
		synchronized(users) {
			if(!users.containsKey(username)) throw new PersistentException("the user "+username+" doesn't exist");
			else users.remove(username);
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
		syncGc();
	}

	long allocatedSpace() {
		return heap.allocatedSpace();
	}

	long maxSpace() {
		return heap.maxSpace();
	}

	public synchronized boolean authenticate(String username, char[] password) {
		byte pw[]=(byte[])system.getUsers().get(username);
		return pw==null?false:Arrays.equals(pw,crypt(new String(password),salt(pw)));
	}

	public synchronized Connection getConnection(CallbackHandler handler) throws RemoteException {
		if(readOnly) throw new PersistentException("store in recovery mode");
		return new Connection(this,Connection.TRANSACTION_READ_UNCOMMITTED,login(handler).getSubject());
	}

	public synchronized AdminConnection getAdminConnection(CallbackHandler handler) throws RemoteException {
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
		system.getTransactions().add(trans);
		return trans;
	}

	synchronized void release(Transaction transaction, Subject subject) {
//		if(readOnly) return;
		if(closed) return;
		system.getTransactions().remove(transaction);
		transaction.rollback(subject);
	}

	public synchronized void close() throws RemoteException {
		if(closed) return;
		UnicastRemoteObject.unexportObject(this,true);
		for(Iterator it=system.getTransactions().iterator();it.hasNext();) {
			((Transaction)it.next()).unlock();
		}
		while(true) {
			try {
				for(Iterator it=connections.keySet().iterator();it.hasNext();it.remove()) {
					((RemoteConnectionImpl)it.next()).close();
				}
				break;
			} catch (ConcurrentModificationException e) {}
		}
		while(true) {
			try {
				for(Iterator it=cache.keySet().iterator();it.hasNext();it.remove()) {
					AccessorImpl obj=get(((Long)it.next()).longValue());
					if(obj!=null) obj.close();
				}
				break;
			} catch (ConcurrentModificationException e) {}
		}
		((RemoteConnectionImpl)systemConnection.connection).close();
		if(!readOnly) heap.mount(false);
		closed=true;
	}

	synchronized void syncGc() {
		if(readOnly) return;
		if(closed) return;
		gc(true);
	}

	public void gc() {
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
		AccessorImpl c=getClass(base);
		if(c!=null) {
			mark(c.base.longValue());
			Iterator t=((PersistentClass)c.object()).fieldIterator();
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
		AccessorImpl c=getClass(patch,base);
		if(c!=null) {
//			Field.CLASS.set(patch,base,new Long(0));
			decRefCount(c.base.longValue());
			Iterator t=((PersistentClass)c.object()).fieldIterator();
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
			long dst=value==null?0:value instanceof AccessorImpl?((AccessorImpl)value).base.longValue():writeObject(value);
			if(dst!=0) incRefCount(dst);
			field.set(heap,base,new Long(dst));
			if(src!=0) decRefCount(src);
		}
	}

	AccessorImpl getClass(long base) {
		return getClass(heap,base);
	}

	AccessorImpl getClass(Heap heap, long base) {
		long ptr=((Long)Field.CLASS.get(heap,base)).longValue();
		return ptr==0?null:ptr==base?AccessorImpl.newInstance(base,new ClassClass(),this):instantiate(ptr);
	}

	void setClass(long base, AccessorImpl clazz) {
		long ptr=clazz==null?base:clazz.base.longValue();
		incRefCount(ptr);
		Field.CLASS.set(heap,base,new Long(ptr));
	}

	AccessorImpl getLock(long base) {
		long ptr=((Long)Field.LOCK.get(heap,base)).longValue();
		return ptr==0?null:instantiate(ptr);
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
