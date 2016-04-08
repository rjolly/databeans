package persistence;

import java.io.Serializable;
import java.util.Iterator;

public class PersistentObject implements Cloneable, Serializable {
	static final String secondary[] = new String[] {"class", "store"};
	transient PersistentClass clazz;
	transient Store store;
	transient Long base;

	public PersistentObject() {
	}

	public PersistentObject(final Store store) {
		this(store, null);
	}

	PersistentObject(final Store store, final PersistentClass clazz) {
		this.store = store;
		this.clazz = clazz == null?store.persistentClass(this):clazz;
		store.create(this);
	}

	protected PersistentClass createClass() {
		return new PersistentClass(this);
	}

	protected String[] secondary() {
		return secondary;
	}

	protected static String[] concat(final String a[], final String b[]) {
		final String c[] = new String[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	public final Object get(String name) {
		return get(clazz.getField(name));
	}

	public final Object set(String name, Object value) {
		return set(clazz.getField(name),value);
	}

	Object get(Field field) {
		return store.get(base,field);
	}

	synchronized Object set(Field field, Object value) {
		Object obj=get(field);
		store.set(base,field,value);
		return obj;
	}

	public Store getStore() {
		return store;
	}

	public int hashCode() {
		return base.hashCode();
	}

	public final boolean equals(Object obj) {
		return this == obj || (obj instanceof PersistentObject && equals((PersistentObject)obj));
	}

	boolean equals(PersistentObject obj) {
		return base.equals(obj.base);
	}

	public String toString() {
		return clazz.name()+"@"+Long.toHexString(base);
	}

	public synchronized Object clone() {
		final PersistentObject obj = store.create(getClass());
		final Iterator t = clazz.fieldIterator();
		while (t.hasNext()) {
			final Field field = (Field)t.next();
			obj.set(field, get(field));
		}
		return obj;
	}

	protected final void finalize() {
		store.release(this);
	}
}
