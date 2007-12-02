package persistence;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;

public interface RemoteSystem extends Persistent {
	Map getUsers() throws RemoteException;
	void setUsers(Map map) throws RemoteException;
	Map getClasses() throws RemoteException;
	void setClasses(Map map) throws RemoteException;
	Collection getTransactions() throws RemoteException;
	void setTransactions(Collection collection) throws RemoteException;
	Object getRoot() throws RemoteException;
	void setRoot(Object obj) throws RemoteException;
}
