package persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import persistence.beans.XMLDecoder;
import persistence.beans.XMLEncoder;

abstract class RemoteAdminConnectionImpl extends RemoteConnectionImpl implements RemoteAdminConnection {
	RemoteAdminConnectionImpl(StoreImpl store, boolean readOnly, Subject subject) throws RemoteException {
		super(store,Connection.TRANSACTION_NONE,readOnly,subject);
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

	public void inport(String name) {
		try {
			XMLDecoder d = new XMLDecoder(connection(),new BufferedInputStream(new FileInputStream(name)));
			setRoot(d.readObject());
			d.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void export(String name) {
		try {
			XMLEncoder e = new XMLEncoder(connection(),new BufferedOutputStream(new FileOutputStream(name)));
			e.writeObject(root());
			e.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
