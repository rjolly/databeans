package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.security.auth.Subject;

public class AdminImpl extends UnicastRemoteObject implements Admin {
	StoreImpl store;
	Subject subject;

	public AdminImpl(StoreImpl store, Subject subject) throws RemoteException {
		this.store=store;
		this.subject=subject;
	}

	public PersistentSystem getSystem() {
		return store.getSystem();
	}

	public void createUser(String username, String password) {
		store.createUser(username,password);
	}

	public void inport(String name) {
		store.inport(name);
	}

	public void export(String name) {
		store.export(name);
	}

	public void close() throws RemoteException {
		store.close();
	}

	public void gc() {
		store.gc();
	}	

	public long allocatedSpace() {
		return store.heap.allocatedSpace();
	}

	public long maxSpace() {
		return store.heap.maxSpace();
	}
}
