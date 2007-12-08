package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;

public abstract class AccessorImpl extends UnicastRemoteObject implements Accessor {
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

	PersistentObject object(Connection connection) {
		PersistentObject obj=clazz.newInstance();
		obj.init(this,connection);
		return obj;
	}

	synchronized Object call(String method, Class types[], Object args[]) {
		try {
			return getClass().getMethod(method,types).invoke(this,args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Object get(String name) {
		return get(clazz.getField(name));
	}

	public Object set(String name, Object value) {
		return set(clazz.getField(name),value);
	}

	public PersistentObject copy() {
		return ((AccessorImpl)clone()).object();
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

	synchronized void kick() {
		notifyAll();
	}

	AccessorImpl getLock() {
		return getLock(0);
	}

	AccessorImpl getLock(int timeout) {
		if(timeout>0 && !store.closing) try {
			wait(timeout);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return store.getLock(base.longValue());
	}

	void setLock(AccessorImpl transaction) {
		store.setLock(base.longValue(),transaction);
	}

	public final Long base() {
		return base;
	}

	public final PersistentClass persistentClass() {
		return clazz;
	}

	public final Store store() {
		return store;
	}

	static AccessorImpl newInstance(Long base, PersistentClass clazz, StoreImpl store) {
		try {
			PersistentObject obj=clazz.newInstance();
			AccessorImpl accessor=obj.createAccessor();
			accessor.init(base,clazz,store);
			obj.init(accessor,store.systemConnection);
			return accessor;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		StringBuffer s=new StringBuffer();
		s.append("[");
		Iterator t=clazz.fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			Object obj=get(field.name);
			s.append(field.name+"="+(obj==object()?"this":obj)+(t.hasNext()?", ":""));
		}
		s.append("]");
		return s.toString();
	}

	public final Object clone() {
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
