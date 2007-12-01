package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class TransientObject extends UnicastRemoteObject implements Persistent {
	Object mutex;

	public TransientObject() throws RemoteException {
		mutex=this;
	}

	public TransientObject(Object mutex) throws RemoteException {
		this.mutex=mutex;
	}

	public String remoteToString() throws RemoteException {
		return "";
	}

	public final String persistentClass() {
		return "";
	}

	protected final Object mutex() {
		return mutex;
	}

	protected LocalWrapper local() {
		return new LocalWrapper(this);
	}
}
