package persistence.util;

import persistence.PersistentClass;

public class AbstractCollectionClass extends PersistentClass {
	protected boolean banned(String property) {
		return super.banned(property) || property.equals("empty");
	}
}
