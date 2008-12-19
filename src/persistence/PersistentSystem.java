package persistence;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import persistence.PersistentObject.MethodCall;
import persistence.util.ArrayList;
import persistence.util.LinkedHashMap;

public class PersistentSystem extends PersistentObject {
	public void init() {
		setUsers((Map)create(LinkedHashMap.class));
		setClasses((Map)create(LinkedHashMap.class));
		setTransactions((List)create(ArrayList.class));
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

	public Map getUsers() {
		return (Map)get("users");
	}

	public void setUsers(Map map) {
		set("users",map);
	}

	public Map getClasses() {
		return (Map)get("classes");
	}

	public void setClasses(Map map) {
		set("classes",map);
	}

	public List getTransactions() {
		return (List)get("transactions");
	}

	public void setTransactions(List list) {
		set("transactions",list);
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
