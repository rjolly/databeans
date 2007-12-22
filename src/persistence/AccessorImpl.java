package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessController;
import java.util.Iterator;

abstract class AccessorImpl extends UnicastRemoteObject implements Accessor {
	static final int TIMEOUT=60000;
	PersistentClass clazz;
	StoreImpl store;
	Long base;

	protected AccessorImpl() throws RemoteException {}

	void init(Long base, PersistentClass clazz, StoreImpl store) {
		this.base=base;
		this.clazz=clazz;
		this.store=store;
	}

	abstract PersistentObject object();

	Object call(String method, Class types[], Object args[]) {
		if(!method.equals("get") && !method.equals("set")) AccessController.checkPermission(new MethodPermission(clazz.name()+"."+method));
		try {
			return getClass().getMethod(method,types).invoke(this,args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Object get(String name) {
		AccessController.checkPermission(new PropertyPermission(clazz.name()+"."+name));
		return get(clazz.getField(name));
	}

	public Object set(String name, Object value) {
		AccessController.checkPermission(new PropertyPermission(clazz.name()+"."+name));
		return set(clazz.getField(name),value);
	}

	Object get(Field field) {
		return attach(store.get(base.longValue(),field));
	}

	Object set(Field field, Object value) {
		Object obj=get(field);
		store.set(base.longValue(),field,detach(value));
		return obj;
	}

	static Object attach(Object obj) {
		return obj instanceof AccessorImpl?((AccessorImpl)obj).object():obj;
	}

	static Object detach(Object obj) {
		return obj instanceof PersistentObject?((PersistentObject)obj).accessor():obj;
	}

	synchronized void lock(AccessorImpl transaction) {
		AccessorImpl t=getLock();
		if(t==null) setLock(transaction);
		else if(t==transaction);
		else {
			t=getLock(TIMEOUT);
			if(t==null) setLock(transaction);
			else throw new PersistentException(this+" locked by "+t.object());
		}
	}

	synchronized void unlock() {
		setLock(null);
		notify();
	}

	AccessorImpl getLock() {
		return getLock(0);
	}

	AccessorImpl getLock(int timeout) {
		if(timeout>0 && !store.closed) try {
			wait(timeout);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return store.getLock(base.longValue());
	}

	void setLock(AccessorImpl transaction) {
		store.setLock(base.longValue(),transaction);
	}

	synchronized void close() throws RemoteException {
		UnicastRemoteObject.unexportObject(this,true);
		object().close();
	}

	public final Long base() {
		return base;
	}

	public final Store store() {
		return store;
	}

	public final PersistentClass persistentClass() {
		return clazz;
	}

	static AccessorImpl newInstance(long base, PersistentClass clazz, StoreImpl store) {
		try {
			PersistentObject obj=clazz.newInstance();
			AccessorImpl accessor=obj.createAccessor();
			accessor.init(new Long(base),clazz,store);
			obj.init(accessor,store.systemConnection);
			return accessor;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public int remoteHashCode() {
		return base.hashCode();
	}

	public boolean remoteEquals(PersistentObject obj) {
		return obj.base().equals(base);
	}

	public String remoteToString() {
		return clazz.name()+"@"+Long.toHexString(base.longValue());
	}

	public PersistentObject remoteClone() {
		return ((AccessorImpl)clone()).object();
	}

	public synchronized final Object clone() {
		AccessorImpl obj=store.create(clazz);
		Iterator t=clazz.fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			set(field,get(field));
		}
		return obj;
	}

	protected final void finalize() {
		store.release(this);
	}
}
