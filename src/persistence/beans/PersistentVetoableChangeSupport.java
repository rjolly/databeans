/*
 * @(#)VetoableChangeSupport.java		1.36 00/02/02
 *
 * Copyright 1996-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package persistence.beans;

import java.beans.*;
import java.util.*;
import java.rmi.*;
import persistence.*;
import persistence.util.*;

public class PersistentVetoableChangeSupport extends PersistentObject implements Remote {
	public RemoteCollection getListeners() {
		return (RemoteCollection)get("listeners");
	}

	public void setListeners(RemoteCollection collection) {
		set("listeners",collection);
	}

	public RemoteMap getChildren() {
		return (RemoteMap)get("children");
	}

	public void setChildren(RemoteMap map) {
		set("children",map);
	}

	public Object getSource() {
		return get("source");
	}

	public void setSource(Object obj) {
		set("source",obj);
	}

	public PersistentVetoableChangeSupport() throws RemoteException {}

	public PersistentVetoableChangeSupport(Accessor accessor, Connection connection, Object sourceBean) throws RemoteException {
		super(accessor,connection);
		if (sourceBean == null) {
			throw new NullPointerException();
		}
		setSource(sourceBean);
	}

	public synchronized void addVetoableChangeListener(RemoteVetoableChangeListener listener) {
		if (getListeners() == null) {
			setListeners((PersistentArrayList)create(PersistentArrayList.class));
		}
		PersistentCollections.localCollection(getListeners()).add(listener);
	}

	public synchronized void removeVetoableChangeListener(RemoteVetoableChangeListener listener) {
		if (getListeners() == null) {
			return;
		}
		PersistentCollections.localCollection(getListeners()).remove(listener);
	}

	public synchronized void addVetoableChangeListener(String propertyName, RemoteVetoableChangeListener listener) {
		if (getChildren() == null) {
			setChildren((PersistentHashMap)create(PersistentHashMap.class));
		}
		PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
		if (child == null) {
			child = (PersistentVetoableChangeSupport)create(PersistentVetoableChangeSupport.class, new Class[] {Object.class}, new Object[] {getSource()});
			PersistentCollections.localMap(getChildren()).put(propertyName, child);
		}
		child.addVetoableChangeListener(listener);
	}

	public synchronized void removeVetoableChangeListener(String propertyName, RemoteVetoableChangeListener listener) {
		if (getChildren() == null) {
			return;
		}
		PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
		if (child == null) {
			return;
		}
		child.removeVetoableChangeListener(listener);
	}

	public void fireVetoableChange(String propertyName, Object oldValue, Object newValue) throws PropertyVetoException {
		if (getListeners() == null && getChildren() == null) {
			return;
		}

	   	PropertyChangeEvent evt = new PropertyChangeEvent(getSource(), propertyName, oldValue, newValue);
		fireVetoableChange(evt);
	}

	public void fireVetoableChange(String propertyName, int oldValue, int newValue) throws PropertyVetoException {
		if (oldValue == newValue) {
			return;
		}
		fireVetoableChange(propertyName, new Integer(oldValue), new Integer(newValue));
	}

	public void fireVetoableChange(String propertyName, boolean oldValue, boolean newValue) throws PropertyVetoException {
		if (oldValue == newValue) {
			return;
		}
		fireVetoableChange(propertyName, new Boolean(oldValue), new Boolean(newValue));
	}

	public void fireVetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {

		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		String propertyName = evt.getPropertyName();
		if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return;
		}

		Collection targets = null;
		PersistentVetoableChangeSupport child = null;
		synchronized (this) {
			if (getListeners() != null) {
				targets = new ArrayList(PersistentCollections.localCollection(getListeners()));
			}
			if (getChildren() != null && propertyName != null) {
				child = (PersistentVetoableChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
			}
		}

		if (getListeners() != null) {
			try {
				Iterator t=targets.iterator();
				while(t.hasNext()) {
					RemoteVetoableChangeListener target = (RemoteVetoableChangeListener)t.next();
					try {
						target.vetoableChange(evt);
					} catch (RemoteException e) {
						PersistentCollections.localCollection(getListeners()).remove(target);
					}
				}
			} catch (PropertyVetoException veto) {
				// Create an event to revert everyone to the old value.
	   			evt = new PropertyChangeEvent(getSource(), propertyName, newValue, oldValue);
				Iterator t=targets.iterator();
				while(t.hasNext()) {
					try {
						RemoteVetoableChangeListener target = (RemoteVetoableChangeListener)t.next();
						try {
							target.vetoableChange(evt);
						} catch (RemoteException e) {
							PersistentCollections.localCollection(getListeners()).remove(target);
						}
					} catch (PropertyVetoException ex) {
						 // We just ignore exceptions that occur during reversions.
					}
				}
				// And now rethrow the PropertyVetoException.
				throw veto;
			}
		}

		if (child != null) {
			child.fireVetoableChange(evt);
		}
	}

	public synchronized boolean hasListeners(String propertyName) {
		if (getListeners() != null && !PersistentCollections.localCollection(getListeners()).isEmpty()) {
			// there is a generic listener
			return true;
		}
		if (getChildren() != null) {
			PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
			if (child != null && child.getListeners() != null) {
				return !PersistentCollections.localCollection(child.getListeners()).isEmpty();
			}
		}
		return false;
	}
}
