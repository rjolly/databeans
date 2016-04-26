import java.beans.ConstructorProperties;
import persistence.PersistentObject;
import persistence.Store;

public class Department extends PersistentObject {
	public Department() {
	}

	@ConstructorProperties({"name"})
	public Department(final Store store, final String name) {
		super(store);
		set("name", name);
	}

	public String getName() {
		return get("name");
	}

	public String toString() {
		return getName();
	}
}
