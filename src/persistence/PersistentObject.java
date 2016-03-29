package persistence;

import java.io.Serializable;
import java.util.Iterator;

public class PersistentObject implements Cloneable, Serializable {
	transient PersistentClass clazz;
	transient Store store;
	transient long base;

	public void init() {}

	static PersistentObject newInstance(long base, PersistentClass clazz, Store store) {
		PersistentObject obj=clazz.newInstance();
		obj.init(base,clazz,store);
		return obj;
	}

	void init(long base, PersistentClass clazz, Store store) {
		this.base=base;
		this.clazz=clazz;
		this.store=store;
	}

	static PersistentObject newInstance(long base) {
		PersistentObject obj=new PersistentObject();
		obj.init(base);
		return obj;
	}

	void init(long base) {
		this.base=base;
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(PersistentClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	protected final PersistentObject create(String name) {
		return store.create(name);
	}

	protected final PersistentObject create(Class clazz) {
		return store.create(clazz);
	}

	protected final PersistentObject create(Class clazz, Class types[], Object args[]) {
		return store.create(clazz,types,args);
	}

	protected final PersistentArray create(Class componentType, int length) {
		return store.create(componentType,length);
	}

	protected final PersistentArray create(Object component[]) {
		return store.create(component);
	}

	protected final PersistentClass get(Class clazz) {
		return store.get(clazz);
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

	public final PersistentClass persistentClass() {
		return clazz;
	}

	public final int hashCode() {
		return new Long(base).hashCode();
	}

	public final boolean equals(Object obj) {
		return this == obj || (obj instanceof PersistentObject && equals((PersistentObject)obj));
	}

	boolean equals(PersistentObject obj) {
		return base==obj.base;
	}

	public String toString() {
		return clazz.name()+"@"+Long.toHexString(base);
	}

	public synchronized final Object clone() {
		PersistentObject obj=store.create(clazz);
		Iterator t=clazz.fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			obj.set(field,get(field));
		}
		return obj;
	}

	protected final void finalize() {
		store.release(this);
	}
}
