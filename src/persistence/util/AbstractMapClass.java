package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;

public final class AbstractMapClass<K,V> extends PersistentClass {
	public AbstractMapClass() {
	}

	public AbstractMapClass(final PersistentObject obj) {
		super(obj);
		set("NULL", new PersistentObject(getStore()));
	}

	V NULL() {
		return getNULL();
	}

	public V getNULL() {
		return get("NULL");
	}
}
