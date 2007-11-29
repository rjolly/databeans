package persistence;

import java.util.Iterator;

public class Accessor {
	static final int TIMEOUT=60000;
	long base;
	PersistentClass clazz;
	StoreImpl store;

	public Accessor(long base, PersistentClass clazz, StoreImpl store) {
		this.base=base;
		this.clazz=clazz;
		this.store=store;
	}

	Object get(Field field) {
		return store.get(base,field);
	}

	void set(Field field, Object value) {
		Accessor t=getLock();
		if(t==null);
		else {
			t=getLock(TIMEOUT);
			if(t==null);
			else throw new PersistentException(this+" locked by "+store.transaction(t));
		}
		store.set(base,field,value);
	}

	synchronized void lock(Accessor transaction) {
		Accessor t=getLock();
		if(t==null) setLock(transaction);
		else if(t==transaction);
		else {
			t=getLock(TIMEOUT);
			if(t==null) setLock(transaction);
			else throw new PersistentException(this+" locked by "+store.transaction(t));
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
		return store.getLock(base);
	}

	void setLock(Accessor accessor) {
		store.setLock(base,accessor);
	}

	public int hashCode() {
		return new Long(base).hashCode();
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof Accessor && base==((Accessor)obj).base);
	}

	protected Object clone() {
		Accessor clone=store.create(clazz);
		copyInto(clone);
		return clone;
	}

	void copyInto(Accessor obj) {
		Iterator t=clazz.fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			store.set(obj.base,field,store.get(base,field));
		}
	}

	public String toString() {
		return Long.toHexString(base);
	}

	protected void finalize() {
		store.release(this);
	}
}
