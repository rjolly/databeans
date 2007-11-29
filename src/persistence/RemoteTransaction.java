package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;
import persistence.util.RemoteCollection;

public interface RemoteTransaction extends Remote {
	String getClient() throws RemoteException;
	void setClient(String str) throws RemoteException;
	RemoteCollection getMethodCalls() throws RemoteException;
	void setMethodCalls(RemoteCollection collection) throws RemoteException;
	RemoteCollection getObjects() throws RemoteException;
	void setObjects(RemoteCollection collection) throws RemoteException;
	String remoteToString() throws RemoteException;
	String persistentClass() throws RemoteException;
}
