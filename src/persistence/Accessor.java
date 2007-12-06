package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface Accessor extends Remote {
	Long base() throws RemoteException;
	PersistentClass persistentClass() throws RemoteException;
}
