package persistence.util;

import persistence.PersistentObject;
import persistence.Store;

public final class TreeSetClass extends AbstractCollectionClass {
	public TreeSetClass() {
	}

	public TreeSetClass(final Store store) {
		super(store, TreeSet.class);
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
