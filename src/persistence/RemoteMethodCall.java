package persistence;

import java.rmi.RemoteException;

public interface RemoteMethodCall extends Persistent {
	Object getTarget() throws RemoteException;
	void setTarget(Object obj) throws RemoteException;
	String getMethod() throws RemoteException;
	void setMethod(String str) throws RemoteException;
	Array getTypes() throws RemoteException;
	void setTypes(Array array) throws RemoteException;
	Array getArgs() throws RemoteException;
	void setArgs(Array array) throws RemoteException;
}
