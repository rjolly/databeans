package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Accessor extends Remote {
	PersistentObject object() throws RemoteException;
	PersistentObject object(Connection connection) throws RemoteException;
	Object call(String method, Class types[], Object args[]) throws RemoteException;
	void lock(Accessor transaction) throws RemoteException;
	void unlock() throws RemoteException;
	void kick() throws RemoteException;
	Long base() throws RemoteException;
	PersistentClass persistentClass() throws RemoteException;
}
