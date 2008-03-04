package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface Accessor extends Remote {
	Long base() throws RemoteException;
	Store store() throws RemoteException;
}
