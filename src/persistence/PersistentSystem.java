package persistence;

import java.util.Map;

public class PersistentSystem extends PersistentObject {
	public PersistentSystem() {
	}

	public PersistentSystem(final Store store) {
		super(store);
	}

	public Map getClasses() {
		return (Map)get("classes");
	}

	public void setClasses(Map map) {
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
