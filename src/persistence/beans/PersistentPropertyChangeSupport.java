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

import java.beans.PropertyChangeEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import persistence.Accessor;
import persistence.Connection;
import persistence.Persistent;
import persistence.PersistentObject;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentHashMap;

public class PersistentPropertyChangeSupport extends PersistentObject implements Persistent {
	public Collection getListeners() {
		return (Collection)get("listeners");
	}

	public void setListeners(Collection collection) {
		set("listeners",collection);
	}

	public Map getChildren() {
		return (Map)get("children");
	}

	public void setChildren(Map map) {
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
			setListeners((List)create(PersistentArrayList.class));
		}
		getListeners().add(listener);
	}

	public synchronized void removePropertyChangeListener(RemotePropertyChangeListener listener) {
		if (getListeners() == null) {
			return;
		}
		getListeners().remove(listener);
	}

	public synchronized void addPropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) {
		if (getChildren() == null) {
			setChildren((Map)create(PersistentHashMap.class));
		}
		PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)getChildren().get(propertyName);
		if (child == null) {
			child = (PersistentPropertyChangeSupport)create(PersistentPropertyChangeSupport.class, new Class[] {Object.class}, new Object[] {getSource()});
			getChildren().put(propertyName, child);
		}
		child.addPropertyChangeListener(listener);
	}

	public synchronized void removePropertyChangeListener(String propertyName, RemotePropertyChangeListener listener) {
		if (getChildren() == null) {
			return;
		}
		PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)getChildren().get(propertyName);
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
				targets = new ArrayList(getListeners());
			}
			if (getChildren() != null && propertyName != null) {
				child = (PersistentPropertyChangeSupport)getChildren().get(propertyName);
			}
		}

		if (targets != null) {
			Iterator t=targets.iterator();
			while(t.hasNext()) {
				RemotePropertyChangeListener target = (RemotePropertyChangeListener)t.next();
				try {
					target.propertyChange(evt);
				} catch (RemoteException e) {
					getListeners().remove(target);
				}
			}
		}

		if (child != null) {
			child.firePropertyChange(evt);
		}		
	}

	public synchronized boolean hasListeners(String propertyName) {
		if (getListeners() != null && !getListeners().isEmpty()) {
			// there is a generic listener
			return true;
		}
		if (getChildren() != null) {
			PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)getChildren().get(propertyName);
			if (child != null && child.getListeners() != null) {
				return !child.getListeners().isEmpty();
			}
		}
		return false;
	}
}
