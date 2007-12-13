package persistence;

import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;

abstract class RemoteAdminImpl extends RemoteConnectionImpl implements RemoteAdmin {
	public RemoteAdminImpl(StoreImpl store, boolean readOnly, Subject subject) throws RemoteException {
		super(store,Connection.TRANSACTION_NONE,readOnly,subject);
	}

	public void changePassword(String oldPassword, String newPassword) {
		store.changePassword(clientName(),oldPassword,newPassword);
	}

	public void changeUserPassword(final String username, final String password) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				AccessController.checkPermission(new AdminPermission("changePassword"));
				store.changePassword(username,password);
				return null;
			}
		},null);
	}

	public void createUser(final String username, final String password) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				AccessController.checkPermission(new AdminPermission("createUser"));
				store.createUser(username,password);
				return null;
			}
		},null);
	}

	public void closeStore() throws RemoteException {
		try {
			Subject.doAsPrivileged(subject,new PrivilegedExceptionAction() {
				public Object run() throws RemoteException {
					AccessController.checkPermission(new AdminPermission("close"));
					store.close();
					return null;
				}
			},null);
		} catch (PrivilegedActionException e) {
			throw (RemoteException)e.getCause();
		}
	}

	public void gc() {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				AccessController.checkPermission(new AdminPermission("gc"));
				store.gc();
				return null;
			}
		},null);
	}

	public long allocatedSpace() {
		return store.heap.allocatedSpace();
	}

	public long maxSpace() {
		return store.heap.maxSpace();
	}
}
