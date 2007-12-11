package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;
import persistence.PersistentObject.MethodCall;

public interface RemoteConnection extends Remote {
	PersistentObject create(PersistentClass clazz, Class types[], Object args[]) throws RemoteException;

	Object getRoot() throws RemoteException;
	void setRoot(Object obj) throws RemoteException;

	int getTransactionIsolation() throws RemoteException;
	void setTransactionIsolation(int level) throws RemoteException;
	boolean isAutoCommit() throws RemoteException;
	void setAutoCommit(boolean autoCommit) throws RemoteException;
	boolean isReadOnly() throws RemoteException;
	void setReadOnly(boolean readOnly) throws RemoteException;

	Object execute(MethodCall call) throws RemoteException;
	Object execute(MethodCall call, MethodCall undo, int index) throws RemoteException;

	void commit() throws RemoteException;
	void rollback() throws RemoteException;
	void close(boolean force) throws RemoteException;

	Admin getAdmin() throws RemoteException;
}