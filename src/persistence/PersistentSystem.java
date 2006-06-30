package persistence;

import java.rmi.*;
import persistence.util.*;

public class PersistentSystem extends PersistentObject implements RemoteSystem {
	public PersistentSystem() throws RemoteException {}

	public PersistentSystem(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
		setUsers((PersistentHashMap)create(PersistentHashMap.class));
		setClasses((PersistentHashMap)create(PersistentHashMap.class));
		setTransactions((PersistentArrayList)create(PersistentArrayList.class));
	}

	public RemoteMap getUsers() {
		return (RemoteMap)get("users");
	}

	public void setUsers(RemoteMap map) {
		set("users",map);
	}

	public RemoteMap getClasses() {
		return (RemoteMap)get("classes");
	}

	public void setClasses(RemoteMap map) {
		set("classes",map);
	}

	public RemoteCollection getTransactions() {
		return (RemoteCollection)get("transactions");
	}

	public void setTransactions(RemoteCollection collection) {
		set("transactions",collection);
	}

	public Object getRoot() {
		return get("root");
	}

	public void setRoot(Object obj) {
		set("root",obj);
	}
}
