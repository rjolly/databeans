package persistence;

import java.rmi.RemoteException;
import javax.security.auth.Subject;

public class AdminConnection extends Connection {
	RemoteAdminConnection connection;

	AdminConnection(StoreImpl store, boolean readOnly, Subject subject) throws RemoteException {
		super(new RemoteAdminConnectionImpl(store,readOnly,subject));
		connection=(RemoteAdminConnection)super.connection;
	}

	public void abortTransaction(Transaction transaction) {
		try {
			connection.abortTransaction(transaction);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void changePassword(String username, String password) {
		changePassword(username,null,password);
	}

	public void changePassword(String username, String oldPassword, String newPassword) {
		try {
			connection.changePassword(username,oldPassword,newPassword);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void addUser(String username, String password) {
		try {
			connection.addUser(username,password);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteUser(String username) {
		try {
			connection.deleteUser(username);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void inport(String name) {
		try {
			connection.inport(name);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void export(String name) {
		try {
			connection.export(name);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void shutdown() {
		try {
			connection.shutdown();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void gc() {
		try {
			connection.gc();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}	

	public long allocatedSpace() {
		try {
			return connection.allocatedSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public long maxSpace() {
		try {
			return connection.maxSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		connection=null;
		super.close();
	}
}
