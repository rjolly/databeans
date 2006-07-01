package persistence;

import java.rmi.*;
import persistence.beans.*;

public interface Notified extends Remote {
	void addPropertyChangeListener(RemotePropertyChangeListener listener) throws RemoteException;
	void removePropertyChangeListener(RemotePropertyChangeListener listener) throws RemoteException;
	void addPropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) throws RemoteException;
	void removePropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) throws RemoteException;
	void addVetoableChangeListener(RemoteVetoableChangeListener listener) throws RemoteException;
	void removeVetoableChangeListener(RemoteVetoableChangeListener listener) throws RemoteException;
	void addVetoableChangeListener(String propertyName, RemoteVetoableChangeListener listener) throws RemoteException;
	void removeVetoableChangeListener(String propertyName, RemoteVetoableChangeListener listener) throws RemoteException;
	boolean hasPropertyChangeListeners(String propertyName) throws RemoteException;
	boolean hasVetoableChangeListeners(String propertyName) throws RemoteException;
}