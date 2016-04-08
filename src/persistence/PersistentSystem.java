package persistence;

import java.util.Map;
import persistence.util.LinkedHashMap;

public class PersistentSystem extends PersistentObject {
	public PersistentSystem() {
	}

	PersistentSystem(final Store store) {
		super(store);
		setClasses(new LinkedHashMap(store));
	}

	public Map getClasses() {
		return (Map)get("classes");
	}

	private void setClasses(Map map) {
		set("classes",map);
	}

	public Object getRoot() {
		return get("root");
	}

	public void setRoot(Object obj) {
		set("root",obj);
	}

	public Object root() {
		return getRoot();
	}
}
