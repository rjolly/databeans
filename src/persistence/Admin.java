package persistence;

import java.rmi.*;

public interface Admin extends Remote {
	RemoteSystem getSystem() throws RemoteException;
	void createUser(String username, String password) throws RemoteException;
	void inport(String name) throws RemoteException;
	void export(String name) throws RemoteException;
	void close() throws RemoteException;
	void gc() throws RemoteException;
	long allocatedSpace() throws RemoteException;
	long maxSpace() throws RemoteException;
}
