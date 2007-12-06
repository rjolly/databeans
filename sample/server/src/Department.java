import persistence.PersistentObject;

public class Department extends PersistentObject {
	public String getName() {
		return (String)get("name");
	}

	public void setName(String s) {
		set("name",s);
	}

	public String toString() {
		return getName();
	}
}
