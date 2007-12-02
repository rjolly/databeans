package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalWrapper implements Serializable {
	Persistent content;

	public LocalWrapper(Persistent content) {
		this.content=content;
	}

	public Persistent content() {
		return content;
	}

	public String toString() {
		try {
			return content.remoteToString();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public String persistentClass() {
		try {
			return content.persistentClass();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
