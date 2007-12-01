package persistence;

import java.rmi.RemoteException;
import persistence.util.RemoteCollection;
import persistence.util.RemoteMap;

public interface RemoteSystem extends Persistent {
	RemoteMap getUsers() throws RemoteException;
	void setUsers(RemoteMap map) throws RemoteException;
	RemoteMap getClasses() throws RemoteException;
	void setClasses(RemoteMap map) throws RemoteException;
	RemoteCollection getTransactions() throws RemoteException;
	void setTransactions(RemoteCollection collection) throws RemoteException;
	Object getRoot() throws RemoteException;
	void setRoot(Object obj) throws RemoteException;
}
