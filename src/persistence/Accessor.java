package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface Accessor extends Remote {
	Long base() throws RemoteException;
	Store store() throws RemoteException;
	PersistentClass persistentClass() throws RemoteException;
	int remoteHashCode() throws RemoteException;
	boolean remoteEquals(PersistentObject obj) throws RemoteException;
	String remoteToString() throws RemoteException;
	PersistentObject remoteClone() throws RemoteException;
}
