package persistence.util;

import persistence.PersistentClass;
import persistence.Store;

public final class LinkedHashMapClass extends PersistentClass {
	public LinkedHashMapClass() {
	}

	public LinkedHashMapClass(final Store store) {
		super(store, LinkedHashMap.class);
	}

	protected boolean secondary(String property) {
		return super.secondary(property) || property.equals("empty");
	}
}
