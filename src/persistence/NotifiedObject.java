package persistence;

import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.rmi.RemoteException;
import persistence.beans.PersistentPropertyChangeSupport;
import persistence.beans.PersistentVetoableChangeSupport;

public abstract class NotifiedObject extends PersistentObject {
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		Object set(Field field, Object value) {
			Object oldValue=get(field);
			try {
				PersistentVetoableChangeSupport support=getVetoableChangeSupport();
				if(support!=null) support.fireVetoableChange(field.name,oldValue,value);
			} catch (PropertyVetoException e) {
				throw new RuntimeException(e);
			}
			PersistentPropertyChangeSupport support=getPropertyChangeSupport();
			if(support!=null) support.firePropertyChange(field.name,oldValue,value);
			return super.set(field,value);
		}
	}

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

	public void init() {
		setPropertyChangeSupport((PersistentPropertyChangeSupport)create(PersistentPropertyChangeSupport.class, new Class[] {Object.class}, new Object[] {this}));
		setVetoableChangeSupport((PersistentVetoableChangeSupport)create(PersistentVetoableChangeSupport.class, new Class[] {Object.class}, new Object[] {this}));
	}

	public final void addPropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(listener);
	}

	public final void removePropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(listener);
	}

	public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
	}

	public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(propertyName, listener);
	}

	public final void addVetoableChangeListener(VetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(listener);
	}

	public final void removeVetoableChangeListener(VetoableChangeListener listener) {
		getVetoableChangeSupport().removeVetoableChangeListener(listener);
	}

	public final void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		getVetoableChangeSupport().addVetoableChangeListener(propertyName, listener);
	}

	public final void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		getVetoableChangeSupport().removeVetoableChangeListener(propertyName, listener);
	}

	public final boolean hasPropertyChangeListeners(String propertyName) {
		return getPropertyChangeSupport().hasListeners(propertyName);
	}

	public final boolean hasVetoableChangeListeners(String propertyName) {
		return getVetoableChangeSupport().hasListeners(propertyName);
	}
}
