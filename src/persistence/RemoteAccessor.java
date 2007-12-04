package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteAccessor extends Remote {
	long base() throws RemoteException;
	PersistentClass persistentClass() throws RemoteException;
}
