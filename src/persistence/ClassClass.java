package persistence;

final class ClassClass extends PersistentClass {
	public String getName() {
		return PersistentClass.class.getName();
	}

	public void setName(String str) {}

	public String getFields() {
		return "L fields;L name";
	}

	public void setFields(String str) {}
}
