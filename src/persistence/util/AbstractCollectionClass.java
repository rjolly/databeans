package persistence.util;

import persistence.PersistentClass;
import persistence.Store;

public class AbstractCollectionClass extends PersistentClass {
	public AbstractCollectionClass() {
	}

	public AbstractCollectionClass(final Store store, final Class clazz) {
		super(store, clazz);
	}

	protected boolean secondary(String property) {
		return super.secondary(property) || property.equals("empty");
	}
}
