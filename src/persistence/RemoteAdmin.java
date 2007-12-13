package persistence;

import java.rmi.RemoteException;

interface RemoteAdmin extends RemoteConnection {
	void changePassword(String oldPassword, String newPassword) throws RemoteException;
	void changeUserPassword(String username, String password) throws RemoteException;
	void createUser(String username, String password) throws RemoteException;
	void closeStore() throws RemoteException;
	void gc() throws RemoteException;
	long allocatedSpace() throws RemoteException;
	long maxSpace() throws RemoteException;
}
