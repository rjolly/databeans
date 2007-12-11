package persistence;

import java.rmi.RemoteException;
import javax.security.auth.Subject;

public class RemoteAdminImpl extends RemoteConnectionImpl implements RemoteAdmin {
	public RemoteAdminImpl(StoreImpl store, Subject subject) throws RemoteException {
		super(store,Connection.TRANSACTION_NONE,subject);
	}

	public void createUser(String username, String password) {
		store.createUser(username,password);
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
