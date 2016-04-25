package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;

public final class HashMapClass<K, V> extends PersistentClass {
	public HashMapClass() {
	}

	public HashMapClass(final PersistentObject obj) {
		super(obj);
		set("NULL_KEY", new PersistentObject(getStore()));
	}

	K NULL_KEY() {
		return getNULL_KEY();
	}

	public K getNULL_KEY() {
		return get("NULL_KEY");
	}
}
