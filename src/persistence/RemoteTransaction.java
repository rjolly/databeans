package persistence;

import java.rmi.*;
import persistence.util.*;

public interface RemoteTransaction extends Remote {
	int getLevel() throws RemoteException;
	boolean isReadOnly() throws RemoteException;
	String getClient() throws RemoteException;
	RemoteMap getDuplicates() throws RemoteException;
	String remoteToString() throws RemoteException;
	String persistentClass() throws RemoteException;
}
