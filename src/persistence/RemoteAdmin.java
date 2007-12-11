package persistence;

import java.rmi.RemoteException;

public interface RemoteAdmin extends RemoteConnection {
	PersistentSystem getSystem() throws RemoteException;
	void createUser(String username, String password) throws RemoteException;
	void inport(String name) throws RemoteException;
	void export(String name) throws RemoteException;
	void close() throws RemoteException;
	void gc() throws RemoteException;
	long allocatedSpace() throws RemoteException;
	long maxSpace() throws RemoteException;
}
