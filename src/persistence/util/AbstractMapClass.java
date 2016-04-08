package persistence.util;

import persistence.PersistentClass;
import persistence.PersistentObject;

public final class AbstractMapClass extends PersistentClass {
	public AbstractMapClass() {
	}

	AbstractMapClass(final PersistentObject obj) {
		super(obj);
		setNULL(new PersistentObject(getStore()));
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
