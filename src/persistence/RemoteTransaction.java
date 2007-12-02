package persistence;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

public interface RemoteTransaction extends Persistent {
	String getClient() throws RemoteException;
	void setClient(String str) throws RemoteException;
	List getMethodCalls() throws RemoteException;
	void setMethodCalls(List list) throws RemoteException;
	Collection getObjects() throws RemoteException;
	void setObjects(Collection collection) throws RemoteException;
}
