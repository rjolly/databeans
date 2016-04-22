package persistence;

import java.util.Iterator;

public class PersistentObject implements Cloneable {
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

	protected final <T> T get(String name) {
		return get(clazz.getField(name));
	}

	protected final <T> T set(String name, T value) {
		return set(clazz.getField(name),value);
	}

	@SuppressWarnings("unchecked")
	<T> T get(Field field) {
		return (T)store.get(base,field);
	}

	synchronized <T> T set(Field field, T value) {
		T obj=get(field);
		store.set(base,field,value);
		return obj;
	}

	protected final Store getStore() {
		return store;
	}

	public int hashCode() {
		return base.hashCode();
	}

	public final boolean equals(Object obj) {
		return this == obj || (obj instanceof PersistentObject && equals((PersistentObject)obj));
	}

	protected boolean equals(PersistentObject obj) {
		return base.equals(obj.base);
	}

	public String toString() {
		return clazz.name()+"@"+Long.toHexString(base);
	}

	public PersistentObject clone() throws CloneNotSupportedException {
		final PersistentObject obj = (PersistentObject)super.clone();
		store.create(obj);
		final Iterator<Field> t = clazz.fieldIterator();
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
