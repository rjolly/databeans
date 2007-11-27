package persistence;

import java.beans.PropertyVetoException;
import java.rmi.RemoteException;
import persistence.beans.PersistentPropertyChangeSupport;
import persistence.beans.PersistentVetoableChangeSupport;
import persistence.beans.RemotePropertyChangeListener;
import persistence.beans.RemoteVetoableChangeListener;

public abstract class NotifiedObject extends PersistentObject {
	public PersistentPropertyChangeSupport getPropertyChangeSupport() {
		return (PersistentPropertyChangeSupport)get("propertyChangeSupport");
	}

	public void setPropertyChangeSupport(PersistentPropertyChangeSupport support) {
		set("propertyChangeSupport",support);
	}

	public PersistentVetoableChangeSupport getVetoableChangeSupport() {
		return (PersistentVetoableChangeSupport)get("vetoableChangeSupport");
	}

	public void setVetoableChangeSupport(PersistentVetoableChangeSupport support) {
		set("vetoableChangeSupport",support);
	}

	public NotifiedObject() throws RemoteException {}

	public NotifiedObject(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
		setPropertyChangeSupport((PersistentPropertyChangeSupport)create(PersistentPropertyChangeSupport.class, new Class[] {Object.class}, new Object[] {this}));
		setVetoableChangeSupport((PersistentVetoableChangeSupport)create(PersistentVetoableChangeSupport.class, new Class[] {Object.class}, new Object[] {this}));
	}

	public final void addPropertyChangeListener(RemotePropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(listener);
	}

	public final void removePropertyChangeListener(RemotePropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(listener);
	}

	public final void addPropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
	}

	public final void removePropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(propertyName, listener);
	}

	public final void addVetoableChangeListener(RemoteVetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(listener);
	}

	public final void removeVetoableChangeListener(RemoteVetoableChangeListener listener) {
		getVetoableChangeSupport().removeVetoableChangeListener(listener);
	}

	public final void addVetoableChangeListener(String propertyName, RemoteVetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(propertyName, listener);
	}

	public final void removeVetoableChangeListener(String propertyName, RemoteVetoableChangeListener listener) {
		getVetoableChangeSupport().removeVetoableChangeListener(propertyName, listener);
	}

	public final boolean hasPropertyChangeListeners(String propertyName) {
		return getPropertyChangeSupport().hasListeners(propertyName);
	}

	public final boolean hasVetoableChangeListeners(String propertyName) {
		return getVetoableChangeSupport().hasListeners(propertyName);
	}

	Object set(Field field, Object value) {
		Object oldValue=get(field);
		try {
			PersistentVetoableChangeSupport support=getVetoableChangeSupport();
			if(support!=null) support.fireVetoableChange(field.name,oldValue,value);
		} catch (PropertyVetoException e) {
			throw new RuntimeException(e);
		}
		{
			PersistentPropertyChangeSupport support=getPropertyChangeSupport();
			if(support!=null) support.firePropertyChange(field.name,oldValue,value);
		}
		return super.set(field,value);
	}
}
