package persistence;

import java.rmi.RemoteException;
import javax.security.auth.Subject;

public class AdminImpl extends ConnectionImpl implements Admin {
	public AdminImpl(StoreImpl store, Subject subject) throws RemoteException {
		super(store,Connection.TRANSACTION_NONE,subject);
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

	public void close() {
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
