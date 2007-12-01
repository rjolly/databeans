package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.transaction.xa.XAResource;

public interface Connection extends Remote {
	static final int TRANSACTION_NONE = 0;
	static final int TRANSACTION_READ_UNCOMMITTED = 1;
//	static final int TRANSACTION_READ_COMMITTED = 2;
//	static final int TRANSACTION_REPEATABLE_READ = 3;
	static final int TRANSACTION_SERIALIZABLE = 4;

	Object create(String name) throws RemoteException;
	Object create(Class clazz) throws RemoteException;
	Object create(Class clazz, Class types[], Object args[]) throws RemoteException;
	Array create(Class componentType, int length) throws RemoteException;
	Array create(Object component[]) throws RemoteException;

	Object getRoot() throws RemoteException;
	void setRoot(Object obj) throws RemoteException;

	int getTransactionIsolation() throws RemoteException;
	void setTransactionIsolation(int level) throws RemoteException;
	boolean isAutoCommit() throws RemoteException;
	void setAutoCommit(boolean autoCommit) throws RemoteException;
	boolean isReadOnly() throws RemoteException;
	void setReadOnly(boolean readOnly) throws RemoteException;

	void commit() throws RemoteException;
	void rollback() throws RemoteException;
	void close() throws RemoteException;
	boolean isClosed() throws RemoteException;

	XAResource getXAResource() throws RemoteException;
}
