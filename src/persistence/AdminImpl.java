package persistence;

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class AdminImpl extends UnicastRemoteObject implements Admin {
	StoreImpl store;

	public AdminImpl(StoreImpl store) throws RemoteException {
		this.store=store;
	}

	public RemoteSystem getSystem() {
		return store.getSystem();
	}

	public void createUser(String username, String password) {
		store.createUser(username,Connections.crypt(password));
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
