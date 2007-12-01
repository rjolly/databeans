package persistence;

import java.rmi.RemoteException;
import persistence.util.RemoteCollection;
import persistence.util.RemoteList;

public interface RemoteTransaction extends Persistent {
	String getClient() throws RemoteException;
	void setClient(String str) throws RemoteException;
	RemoteList getMethodCalls() throws RemoteException;
	void setMethodCalls(RemoteList list) throws RemoteException;
	RemoteCollection getObjects() throws RemoteException;
	void setObjects(RemoteCollection collection) throws RemoteException;
}
