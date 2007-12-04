package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;

public class Accessor extends UnicastRemoteObject {
	static final int TIMEOUT=60000;
	protected final PersistentObject object;
	PersistentClass clazz;
	StoreImpl store;
	Long base;

	public Accessor(PersistentObject object) throws RemoteException {
		this.object=object;
	}

	void init(Long base, PersistentClass clazz, StoreImpl store) {
		this.base=base;
		this.clazz=clazz;
		this.store=store;
	}

	static Accessor create(Long base, PersistentClass clazz, StoreImpl store) {
		PersistentObject obj=clazz.newInstance();
		Accessor accessor=obj.accessor();
		accessor.init(base,clazz,store);
		obj.init(accessor,store.systemConnection);
		return accessor;
	}

	PersistentObject object(ConnectionImpl connection) {
		PersistentObject obj=clazz.newInstance();
		obj.init(this,connection);
		return obj;
	}

	Object call(String method, Class types[], Object args[]) {
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

	Object get(Field field) {
		return attach(store.get(base.longValue(),field));
	}

	synchronized Object set(Field field, Object value) {
		Accessor t=getLock();
		if(t==null);
		else {
			t=getLock(TIMEOUT);
			if(t==null);
			else throw new PersistentException(this+" locked by "+t.object);
		}
		Object obj=get(field);
		store.set(base.longValue(),field,detach(value));
		return obj;
	}

	static Object attach(Object obj) {
		return obj instanceof Accessor?((Accessor)obj).object:obj;
	}

	static Object detach(Object obj) {
		return obj instanceof PersistentObject?((PersistentObject)obj).accessor:obj;
	}

	synchronized void lock(Accessor transaction) {
		Accessor t=getLock();
		if(t==null) setLock(transaction);
		else if(t==transaction);
		else {
			t=getLock(TIMEOUT);
			if(t==null) setLock(transaction);
			else throw new PersistentException(this+" locked by "+t.object);
		}
	}

	synchronized void unlock() {
		setLock(null);
		notify();
	}

	synchronized void kick() {
		notifyAll();
	}

	Accessor getLock() {
		return getLock(0);
	}

	Accessor getLock(int timeout) {
		if(timeout>0 && !store.closing) try {
			wait(timeout);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return store.getLock(base.longValue());
	}

	void setLock(Accessor accessor) {
		store.setLock(base.longValue(),accessor);
	}

	public String toString() {
		return Long.toHexString(base.longValue());
	}

	public Object clone() {
		Accessor obj=store.create(clazz);
		Iterator t=clazz.fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			set(field,get(field));
		}
		return obj;
	}

	protected void finalize() {
		store.release(this);
	}
}
