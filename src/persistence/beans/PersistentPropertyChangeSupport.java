/*
 * @(#)PropertyChangeSupport.java		1.34 00/02/02
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

public class PersistentPropertyChangeSupport extends PersistentObject implements Remote {
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

	public PersistentPropertyChangeSupport() throws RemoteException {}

	public PersistentPropertyChangeSupport(Accessor accessor, Connection connection, Object sourceBean) throws RemoteException {
		super(accessor,connection);
		if (sourceBean == null) {
			throw new NullPointerException();
		}
		setSource(sourceBean);
	}

	public synchronized void addPropertyChangeListener(RemotePropertyChangeListener listener) {
		if (getListeners() == null) {
			setListeners((PersistentArrayList)create(PersistentArrayList.class));
		}
		PersistentCollections.localCollection(getListeners()).add(listener);
	}

	public synchronized void removePropertyChangeListener(RemotePropertyChangeListener listener) {
		if (getListeners() == null) {
			return;
		}
		PersistentCollections.localCollection(getListeners()).remove(listener);
	}

	public synchronized void addPropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) {
		if (getChildren() == null) {
			setChildren((PersistentHashMap)create(PersistentHashMap.class));
		}
		PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
		if (child == null) {
			child = (PersistentPropertyChangeSupport)create(PersistentPropertyChangeSupport.class, new Class[] {Object.class}, new Object[] {getSource()});
			PersistentCollections.localMap(getChildren()).put(propertyName, child);
		}
		child.addPropertyChangeListener(listener);
	}

	public synchronized void removePropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) {
		if (getChildren() == null) {
			return;
		}
		PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
		if (child == null) {
			return;
		}
		child.removePropertyChangeListener(listener);
	}

	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		if (getListeners() == null && getChildren() == null) {
			return;
		}

	   	PropertyChangeEvent evt = new PropertyChangeEvent(getSource(), propertyName, oldValue, newValue);
		firePropertyChange(evt);
	}

	public void firePropertyChange(String propertyName, int oldValue, int newValue) {
		if (oldValue == newValue) {
			return;
		}
		firePropertyChange(propertyName, new Integer(oldValue), new Integer(newValue));
	}

	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
		if (oldValue == newValue) {
			return;
		}
		firePropertyChange(propertyName, new Boolean(oldValue), new Boolean(newValue));
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		String propertyName = evt.getPropertyName();

		if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return;
		}

		Collection targets = null;
		PersistentPropertyChangeSupport child = null;
		synchronized (this) {
			if (getListeners() != null) {
				targets = new ArrayList(PersistentCollections.localCollection(getListeners()));
			}
			if (getChildren() != null && propertyName != null) {
				child = (PersistentPropertyChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
			}
		}

		if (targets != null) {
			Iterator t=targets.iterator();
			while(t.hasNext()) {
				RemotePropertyChangeListener target = (RemotePropertyChangeListener)t.next();
				try {
					target.propertyChange(evt);
				} catch (RemoteException e) {
					PersistentCollections.localCollection(getListeners()).remove(target);
				}
			}
		}

		if (child != null) {
			child.firePropertyChange(evt);
		}		
	}

	public synchronized boolean hasListeners(String propertyName) {
		if (getListeners() != null && !PersistentCollections.localCollection(getListeners()).isEmpty()) {
			// there is a generic listener
			return true;
		}
		if (getChildren() != null) {
			PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)PersistentCollections.localMap(getChildren()).get(propertyName);
			if (child != null && child.getListeners() != null) {
				return !PersistentCollections.localCollection(child.getListeners()).isEmpty();
			}
		}
		return false;
	}
}
