package persistence;

import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;

class RemoteAdminConnection extends RemoteConnection {
	RemoteAdminConnection(AdminConnection connection, Store store, boolean readOnly, Subject subject) throws RemoteException {
		super(connection,store,readOnly,subject);
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
