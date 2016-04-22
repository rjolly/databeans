package persistence;

final class ClassClass extends PersistentClass {
	transient String fields;

	ClassClass() {
		init(PersistentClass.class);
	}

	public String getName() {
		return name;
	}

	public void setName(String str) {
		name=str;
	}

	public String getFields() {
		return fields;
	}

	public void setFields(String str) {
		fields=str;
	}
}
