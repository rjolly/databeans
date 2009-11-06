package persistence;

import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;

class RemoteAdminConnectionImpl extends RemoteConnectionImpl implements RemoteAdminConnection {
	RemoteAdminConnectionImpl(AdminConnection connection, StoreImpl store, boolean readOnly, Subject subject) throws RemoteException {
		super(connection,store,readOnly,subject);
	}

	public void abortTransaction(Transaction transaction) {
		abortTransaction((Transaction)store.attach(transaction),true);
	}

	void abortTransaction(final Transaction transaction, boolean attached) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				store.abortTransaction(transaction);
				return null;
			}
		},null);
	}

	public void changePassword(final String username, final String oldPassword, final String newPassword) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				store.changePassword(username,oldPassword,newPassword);
				return null;
			}
		},null);
	}

	public void addUser(final String username, final String password) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				store.addUser(username,password);
				return null;
			}
		},null);
	}

	public void deleteUser(final String username) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				store.deleteUser(username);
				return null;
			}
		},null);
	}

	public void inport(final String name) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				store.inport(name);
				return null;
			}
		},null);
	}

	public void export(final String name) {
		Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				store.export(name);
				return null;
			}
		},null);
	}

	public void shutdown() throws RemoteException {
		try {
			Subject.doAsPrivileged(subject,new PrivilegedExceptionAction() {
				public Object run() throws RemoteException {
					store.shutdown();
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
				store.userGc();
				return null;
			}
		},null);
	}

	public long allocatedSpace() {
		return store.allocatedSpace();
	}

	public long maxSpace() {
		return store.maxSpace();
	}
}
