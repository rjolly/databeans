package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface Accessor extends Remote {
	long base() throws RemoteException;
	Store store() throws RemoteException;
}
