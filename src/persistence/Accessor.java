package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface Accessor extends Remote {
	long base() throws RemoteException;
	PersistentClass persistentClass() throws RemoteException;
}
