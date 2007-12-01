package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Persistent extends Remote {
	String remoteToString() throws RemoteException;
	String persistentClass() throws RemoteException;
}
