package persistence;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import persistence.PersistentObject.MethodCall;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentLinkedHashMap;

public class PersistentSystem extends PersistentObject {
	public void init() {
		setUsers((Map)create(PersistentLinkedHashMap.class));
		setClasses((Map)create(PersistentLinkedHashMap.class));
		setTransactions((Collection)create(PersistentArrayList.class));
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

	public Collection getTransactions() {
		return (Collection)get("transactions");
	}

	public void setTransactions(Collection collection) {
		set("transactions",collection);
	}

	public Object getRoot() {
		return get("root");
	}

	public void setRoot(Object obj) {
		set("root",obj);
	}

	public Object root() {
		return execute(
			new MethodCall("root",new Class[] {},new Object[] {}));
	}
}
