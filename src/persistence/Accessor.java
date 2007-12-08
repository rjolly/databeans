package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Accessor extends Remote {
	Long base() throws RemoteException;
	PersistentClass persistentClass() throws RemoteException;
	Store store() throws RemoteException;
}
