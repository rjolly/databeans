package persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import javax.security.auth.Subject;
import persistence.beans.XMLDecoder;
import persistence.beans.XMLEncoder;

public class AdminConnection extends Connection {
	AdminConnection(StoreImpl store, boolean readOnly, Subject subject) throws RemoteException {
		connection=new RemoteAdmin(store,readOnly,subject);
	}

	class RemoteAdmin extends RemoteAdminConnectionImpl {
		public RemoteAdmin(StoreImpl store, boolean readOnly, Subject subject) throws RemoteException {
			super(store,readOnly,subject);
		}

		Connection connection() {
			return AdminConnection.this;
		}
	}

	public void changePassword(String username, String password) {
		changePassword(username,null,password);
	}

	public void changePassword(String username, String oldPassword, String newPassword) {
		try {
			((persistence.RemoteAdminConnection)connection).changePassword(username,oldPassword,newPassword);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void addUser(String username, String password) {
		try {
			((persistence.RemoteAdminConnection)connection).addUser(username,password);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteUser(String username) {
		try {
			((persistence.RemoteAdminConnection)connection).deleteUser(username);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void inport(String name) {
		try {
			XMLDecoder d = new XMLDecoder(this,new BufferedInputStream(new FileInputStream(name)));
			setRoot(d.readObject());
			d.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void export(String name) {
		try {
			XMLEncoder e = new XMLEncoder(this,new BufferedOutputStream(new FileOutputStream(name)));
			e.writeObject(root());
			e.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void closeStore() {
		try {
			((persistence.RemoteAdminConnection)connection).closeStore();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void gc() {
		try {
			((persistence.RemoteAdminConnection)connection).gc();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}	

	public long allocatedSpace() {
		try {
			return ((persistence.RemoteAdminConnection)connection).allocatedSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public long maxSpace() {
		try {
			return ((persistence.RemoteAdminConnection)connection).maxSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
