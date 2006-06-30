package persistence;

import java.rmi.*;
import persistence.util.*;

public interface RemoteSystem extends Remote {
	RemoteMap getUsers() throws RemoteException;
	RemoteMap getClasses() throws RemoteException;
	RemoteCollection getTransactions() throws RemoteException;
	Object getRoot() throws RemoteException;
	String remoteToString() throws RemoteException;
	String persistentClass() throws RemoteException;
}
