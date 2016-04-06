package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;
import persistence.Store;

public final class HashMapClass extends PersistentClass {
	public HashMapClass() {
	}

	public HashMapClass(final Store store) {
		super(store, HashMap.class);
		setNULL_KEY(new PersistentObject(store));
	}

	protected boolean secondary(String property) {
		return super.secondary(property) || property.equals("empty");
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
