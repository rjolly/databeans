package persistence;

import java.rmi.RemoteException;
import java.util.Map;
import persistence.util.LinkedHashMap;

public class PersistentSystem extends PersistentObject {
	public void init() {
		setClasses((Map)create(LinkedHashMap.class));
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		public Object root() {
			return getRoot();
		}
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
		return executeAtomic(
			new MethodCall("root",new Class[] {},new Object[] {}));
	}
}
