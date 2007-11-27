package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteMethodCall extends Remote {
	Remote getTarget() throws RemoteException;
	void setTarget(Remote obj) throws RemoteException;
	String getMethod() throws RemoteException;
	void setMethod(String str) throws RemoteException;
	RemoteArray getTypes() throws RemoteException;
	void setTypes(RemoteArray array) throws RemoteException;
	RemoteArray getArgs() throws RemoteException;
	void setArgs(RemoteArray array) throws RemoteException;
	String remoteToString() throws RemoteException;
	String persistentClass() throws RemoteException;
}
