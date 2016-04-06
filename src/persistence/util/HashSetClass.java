package persistence.util;

import persistence.PersistentObject;
import persistence.Store;

public final class HashSetClass extends AbstractCollectionClass {
	public HashSetClass() {
	}

	public HashSetClass(final Store store) {
		super(store, HashSet.class);
		setPRESENT(new PersistentObject(store));
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
