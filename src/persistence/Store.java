package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Store extends Remote {
	Connection getConnection(String username, byte[] password) throws RemoteException;
	char salt(String username) throws RemoteException;
}
