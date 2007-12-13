package persistence;

import java.rmi.RemoteException;
import javax.security.auth.Subject;

abstract class RemoteAdminImpl extends RemoteConnectionImpl implements RemoteAdmin {
	public RemoteAdminImpl(StoreImpl store, boolean readOnly, Subject subject) throws RemoteException {
		super(store,Connection.TRANSACTION_NONE,readOnly,subject);
	}

	public void changePassword(String oldPassword, String newPassword) {
		store.changePassword(clientName(),oldPassword,newPassword);
	}

	public void changeUserPassword(String username, String password) {
		store.changePassword(username,password);
	}

	public void createUser(String username, String password) {
		store.createUser(username,password);
	}

	public void closeStore() throws RemoteException {
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
