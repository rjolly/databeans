package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.transaction.xa.XAResource;

public interface Connection extends Remote {
	static final int TRANSACTION_NONE = 0;
	static final int TRANSACTION_READ_UNCOMMITTED = 1;
	static final int TRANSACTION_READ_COMMITTED = 2;
	static final int TRANSACTION_REPEATABLE_READ = 3;
	static final int TRANSACTION_SERIALIZABLE = 4;

	PersistentObject create(String name) throws RemoteException;
	PersistentObject create(Class clazz) throws RemoteException;
	PersistentObject create(Class clazz, Class types[], Object args[]) throws RemoteException;
	PersistentArray create(Class componentType, int length) throws RemoteException;
	PersistentArray create(Object component[]) throws RemoteException;
	PersistentObject create(PersistentObject obj) throws RemoteException;

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
	void close() throws RemoteException;
	boolean isClosed() throws RemoteException;

	XAResource getXAResource() throws RemoteException;
}
