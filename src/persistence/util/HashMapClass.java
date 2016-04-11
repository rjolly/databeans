package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;

public final class HashMapClass<K, V> extends PersistentClass {
	public HashMapClass() {
	}

	@SuppressWarnings("unchecked")
	HashMapClass(final PersistentObject obj) {
		super(obj);
		setNULL_KEY((K)new PersistentObject(getStore()));
	}

	K NULL_KEY() {
		return getNULL_KEY();
	}

	public K getNULL_KEY() {
		return get("NULL_KEY");
	}

	private void setNULL_KEY(K obj) {
		set("NULL_KEY",obj);
	}
}
