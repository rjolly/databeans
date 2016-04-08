package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;

public final class HashSetClass extends PersistentClass {
	public HashSetClass() {
	}

	HashSetClass(final PersistentObject obj) {
		super(obj);
		setPRESENT(new PersistentObject(getStore()));
	}

	// Dummy value to associate with an Object in the backing Map
	PersistentObject PRESENT() {
		return getPRESENT();
	}

	public PersistentObject getPRESENT() {
		return (PersistentObject)get("PRESENT");
	}

	private void setPRESENT(PersistentObject obj) {
		set("PRESENT",obj);
	}
}
