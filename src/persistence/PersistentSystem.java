package persistence;

import java.util.Map;
import persistence.util.LinkedHashMap;

public class PersistentSystem extends PersistentObject {
	public PersistentSystem() {
	}

	PersistentSystem(final Store store) {
		super(store);
		setClasses(new LinkedHashMap<String, PersistentClass>(store));
	}

	public Map<String, PersistentClass> getClasses() {
		return get("classes");
	}

	private void setClasses(Map<String, PersistentClass> map) {
		set("classes",map);
	}

	public <T> T getRoot() {
		return get("root");
	}

	public <T> void setRoot(final T obj) {
		set("root",obj);
	}
}
