package persistence;

import java.rmi.RemoteException;

interface RemoteAdminConnection extends RemoteConnection {
	void changePassword(String username, String oldPassword, String newPassword) throws RemoteException;
	void createUser(String username, String password) throws RemoteException;
	void closeStore() throws RemoteException;
	void gc() throws RemoteException;
	long allocatedSpace() throws RemoteException;
	long maxSpace() throws RemoteException;
}
