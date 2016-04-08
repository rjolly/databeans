package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;

public final class HashMapClass extends PersistentClass {
	public HashMapClass() {
	}

	HashMapClass(final PersistentObject obj) {
		super(obj);
		setNULL_KEY(new PersistentObject(getStore()));
	}

	PersistentObject NULL_KEY() {
		return getNULL_KEY();
	}

	public PersistentObject getNULL_KEY() {
		return (PersistentObject)get("NULL_KEY");
	}

	private void setNULL_KEY(PersistentObject obj) {
		set("NULL_KEY",obj);
	}
}
