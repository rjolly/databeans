package persistence;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentHashMap;

public class PersistentSystem extends PersistentObject implements RemoteSystem {
	public PersistentSystem() throws RemoteException {}

	public PersistentSystem(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
		setUsers((Map)create(PersistentHashMap.class));
		setClasses((Map)create(PersistentHashMap.class));
		setTransactions((Collection)create(PersistentArrayList.class));
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
}
