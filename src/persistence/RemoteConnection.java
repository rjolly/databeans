package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.security.auth.Subject;
import persistence.PersistentObject.MethodCall;

interface RemoteConnection extends Remote {
	PersistentObject create(PersistentClass clazz, Class types[], Object args[]) throws RemoteException;
	PersistentClass get(String name) throws RemoteException;
	PersistentClass get(Class clazz) throws RemoteException;
	PersistentClass get(Class componentType, int length) throws RemoteException;

	PersistentSystem system() throws RemoteException;
	Subject subject() throws RemoteException;

	int getTransactionIsolation() throws RemoteException;
	void setTransactionIsolation(int level) throws RemoteException;
	boolean isReadOnly() throws RemoteException;
	void setReadOnly(boolean readOnly) throws RemoteException;
	boolean isAutoCommit() throws RemoteException;
	void setAutoCommit(boolean autoCommit) throws RemoteException;

	Object execute(MethodCall call) throws RemoteException;
	Object executeAtomic(MethodCall call) throws RemoteException;
	Object executeAtomic(MethodCall call, MethodCall undo, int index) throws RemoteException;

	void commit() throws RemoteException;
	void rollback() throws RemoteException;
}
