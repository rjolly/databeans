package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;

public final class AbstractMapClass<K,V> extends PersistentClass {
	public AbstractMapClass() {
	}

	@SuppressWarnings("unchecked")
	AbstractMapClass(final PersistentObject obj) {
		super(obj);
		setNULL((V)new PersistentObject(getStore()));
	}

	V NULL() {
		return getNULL();
	}

	public V getNULL() {
		return get("NULL");
	}

	private void setNULL(V obj) {
		set("NULL",obj);
	}
}
