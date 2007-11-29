package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class TransientObject extends UnicastRemoteObject {
	Object mutex;

	public TransientObject(Object mutex) throws RemoteException {
		this.mutex=mutex;
	}

	protected final Object mutex() {
		return mutex;
	}
}
