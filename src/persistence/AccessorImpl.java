package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

abstract class AccessorImpl extends UnicastRemoteObject implements Accessor {
	protected AccessorImpl() throws RemoteException {}

	abstract PersistentObject object();

	abstract Object call(String method, Class types[], Object args[], boolean check);

	abstract Object get(Field field);

	abstract Object set(Field field, Object value);

	public abstract long base();

	public abstract Store store();

	public abstract PersistentClass persistentClass();

	synchronized void close() throws RemoteException {
		UnicastRemoteObject.unexportObject(this,true);
	}
}
