package persistence;

import java.rmi.RemoteException;

public class AdminConnection extends Connection {
	RemoteAdminConnection connection;

	AdminConnection() {}

	public void inport(String name) {
		connection.inport(name);
	}

	public void export(String name) {
		connection.export(name);
	}

	public void shutdown() {
		try {
			connection.shutdown();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void gc() {
		connection.gc();
	}	

	public long allocatedSpace() {
		return connection.allocatedSpace();
	}

	public long maxSpace() {
		return connection.maxSpace();
	}
}
