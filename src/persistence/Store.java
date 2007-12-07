package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.security.auth.callback.CallbackHandler;

public interface Store extends Remote {
	boolean authenticate(String username, char[] password) throws RemoteException;
	Connection getConnection(CallbackHandler handler) throws RemoteException;
	Admin getAdmin(CallbackHandler handler) throws RemoteException;
}
