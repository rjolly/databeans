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

public class Admin extends Connection {
	RemoteAdmin admin;

	Admin(StoreImpl store, Subject subject) throws RemoteException {
		super(new RemoteAdminImpl(store,subject));
		admin=(RemoteAdmin)connection;
	}

	public PersistentSystem getSystem() {
		return super.getSystem();
	}

	public void createUser(String username, String password) {
		try {
			admin.createUser(username,password);
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
			e.writeObject(getRoot());
			e.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		try {
			admin.close();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void gc() {
		try {
			admin.gc();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}	

	public long allocatedSpace() {
		try {
			return admin.allocatedSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public long maxSpace() {
		try {
			return admin.maxSpace();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
