import persistence.PersistentObject;
import persistence.Store;

public class Department extends PersistentObject {
	public Department() {
	}

	public Department(final Store store) {
		super(store);
	}

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
