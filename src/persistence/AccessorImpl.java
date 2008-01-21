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

	static AccessorImpl newInstance(long base, PersistentClass clazz, StoreImpl store) {
		try {
			PersistentObject obj=clazz.newInstance();
			AccessorImpl accessor=obj.createAccessor();
			accessor.base=new Long(base);
			accessor.clazz=clazz;
			accessor.store=store;
			obj.accessor=accessor;
			obj.connection=store.systemConnection;
			return accessor;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	void setClass(PersistentClass clazz) {
		this.clazz=clazz;
	}

	abstract PersistentObject object();

	Object call(String method, Class types[], Object args[], boolean check) {
		if(check) {
			if(method.equals("get") || method.equals("set")) AccessController.checkPermission(new PropertyPermission(clazz.name()+"."+args[0]));
			else AccessController.checkPermission(new MethodPermission(clazz.name()+"."+method));
		}
		try {
			return getClass().getMethod(method,types).invoke(this,args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public final Object get(String name) {
		return get(clazz.getField(name));
	}

	public final Object set(String name, Object value) {
		return set(clazz.getField(name),value);
	}

	Object get(Field field) {
		return attach(store.get(base.longValue(),field));
	}

	synchronized Object set(Field field, Object value) {
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

	public final long base() {
		return base.longValue();
	}

	public final Store store() {
		return store;
	}

	public final PersistentClass persistentClass() {
		return clazz;
	}

	public int persistentHashCode() {
		return base.hashCode();
	}

	public boolean persistentEquals(PersistentObject obj) {
		return base.equals(obj.accessor().base);
	}

	public String persistentToString() {
		return clazz.name()+"@"+Long.toHexString(base.longValue());
	}

	public PersistentObject persistentClone() {
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
