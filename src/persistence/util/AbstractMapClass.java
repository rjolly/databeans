package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;
import persistence.Store;

public final class AbstractMapClass extends PersistentClass {
	public AbstractMapClass() {
	}

	public AbstractMapClass(final Store store, final Class clazz) {
		super(store, clazz);
		setNULL(new PersistentObject(store));
	}

	protected boolean secondary(String property) {
		return super.secondary(property) || property.equals("empty");
	}

	PersistentObject NULL() {
		return getNULL();
	}

	public PersistentObject getNULL() {
		return (PersistentObject)get("NULL");
	}

	private void setNULL(PersistentObject obj) {
		set("NULL",obj);
	}
}
