package persistence;

import java.rmi.RemoteException;

interface RemoteAdminConnection extends RemoteConnection {
	void changePassword(String username, String oldPassword, String newPassword) throws RemoteException;
	void addUser(String username, String password) throws RemoteException;
	void deleteUser(String username) throws RemoteException;
	void closeStore() throws RemoteException;
	void gc() throws RemoteException;
	long allocatedSpace() throws RemoteException;
	long maxSpace() throws RemoteException;
}