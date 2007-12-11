package persistence;

import java.rmi.RemoteException;
import javax.security.auth.Subject;

public class Admin extends Connection {
	RemoteAdmin admin;

	Admin(StoreImpl store, Subject subject) throws RemoteException {
		super(new RemoteAdminImpl(store,subject));
	}

	public PersistentSystem getSystem() {
		try {
			return admin.getSystem();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void createUser(String username, String password) {
		try {
			admin.createUser(username,password);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void inport(String name) {
		try {
			admin.inport(name);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void export(String name) {
		try {
			admin.export(name);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		try {
			admin.close();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void gc() {
		try {
			admin.gc();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}	

	public long allocatedSpace() {
		try {
			return admin.allocatedSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public long maxSpace() {
		try {
			return admin.maxSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
