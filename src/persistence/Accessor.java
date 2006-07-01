package persistence;

import java.util.*;

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

	synchronized void set(Field field, Object value) {
		Transaction t;
		t=getLock();
		if(t==null);
		else {
			try {
				wait(TIMEOUT);
			} catch (InterruptedException e) {
				throw new PersistentException(lockedBy(t));
			}
			t=getLock();
			if(t==null);
			else throw new PersistentException(lockedBy(t));
		}
		store.set(base,field,value);
	}

	synchronized void lock(Transaction transaction) {
		Transaction t;
		t=getLock();
		if(t==null) setLock(transaction);
		else if(t.accessor==transaction.accessor);
		else {
			try {
				wait(TIMEOUT);
			} catch (InterruptedException e) {
				throw new PersistentException(lockedBy(t));
			}
			t=getLock();
			if(t==null) setLock(transaction);
			else throw new PersistentException(lockedBy(t));
		}
	}

	synchronized void unlock() {
		setLock(null);
		notify();
	}

	Transaction getLock() {
		return store.getLock(base);
	}

	void setLock(Transaction t) {
		store.setLock(base,t);
	}

	String lockedBy(Transaction t) {
		return ""+this+" locked by "+t;
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