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
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import persistence.Accessor;
import persistence.MethodCall;
import persistence.PersistentObject;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentHashMap;

public class PersistentPropertyChangeSupport extends PersistentObject {
	protected Accessor accessor() throws RemoteException {
		return new PropertyChangeSupportAccessor(this);
	}

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

	protected void init(Object sourceBean) throws RemoteException {
		if (sourceBean == null) {
			throw new NullPointerException();
		}
		setSource(sourceBean);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		execute(
			new MethodCall(this,"addPropertyChangeListener",new Class[] {PropertyChangeListener.class},new Object[] {listener}));
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		execute(
			new MethodCall(this,"removePropertyChangeListener",new Class[] {PropertyChangeListener.class},new Object[] {listener}));
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		execute(
			new MethodCall(this,"addPropertyChangeListener",new Class[] {String.class,PropertyChangeListener.class},new Object[] {propertyName,listener}));
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		execute(
			new MethodCall(this,"removePropertyChangeListener",new Class[] {String.class,PropertyChangeListener.class},new Object[] {propertyName,listener}));
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

		Collection targets = getTargets();
		PersistentPropertyChangeSupport child = getChild(propertyName);

		if (targets != null) {
			Iterator t=targets.iterator();
			while(t.hasNext()) {
				PropertyChangeListener target = (PropertyChangeListener)t.next();
				target.propertyChange(evt);
			}
		}

		if (child != null) {
			child.firePropertyChange(evt);
		}		
	}

	Collection getTargets() {
		return (Collection)execute(
			new MethodCall(this,"getTargets",new Class[] {},new Object[] {}));
	}

	PersistentPropertyChangeSupport getChild(String propertyName) {
		return (PersistentPropertyChangeSupport)execute(
			new MethodCall(this,"getChild",new Class[] {String.class},new Object[] {propertyName}));
	}

	public boolean hasListeners(String propertyName) {
		return ((Boolean)execute(
			new MethodCall(this,"hasListeners",new Class[] {String.class},new Object[] {propertyName}))).booleanValue();
	}
}

class PropertyChangeSupportAccessor extends Accessor {
	PropertyChangeSupportAccessor(PersistentObject object) throws RemoteException {
		super(object);
	}

	public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
		if (((PersistentPropertyChangeSupport)object).getListeners() == null) {
			((PersistentPropertyChangeSupport)object).setListeners((List)((PersistentPropertyChangeSupport)object).create(PersistentArrayList.class));
		}
		((PersistentPropertyChangeSupport)object).getListeners().add(listener);
	}

	public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
		if (((PersistentPropertyChangeSupport)object).getListeners() == null) {
			return;
		}
		((PersistentPropertyChangeSupport)object).getListeners().remove(listener);
	}

	public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (((PersistentPropertyChangeSupport)object).getChildren() == null) {
			((PersistentPropertyChangeSupport)object).setChildren((Map)((PersistentPropertyChangeSupport)object).create(PersistentHashMap.class));
		}
		PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)((PersistentPropertyChangeSupport)object).getChildren().get(propertyName);
		if (child == null) {
			child = (PersistentPropertyChangeSupport)((PersistentPropertyChangeSupport)object).create(PersistentPropertyChangeSupport.class, new Class[] {Object.class}, new Object[] {((PersistentPropertyChangeSupport)object).getSource()});
			((PersistentPropertyChangeSupport)object).getChildren().put(propertyName, child);
		}
		child.addPropertyChangeListener(listener);
	}

	public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (((PersistentPropertyChangeSupport)object).getChildren() == null) {
			return;
		}
		PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)((PersistentPropertyChangeSupport)object).getChildren().get(propertyName);
		if (child == null) {
			return;
		}
		child.removePropertyChangeListener(listener);
	}

	public synchronized Collection getTargets() {
		return ((PersistentPropertyChangeSupport)object).getListeners() != null?new ArrayList(((PersistentPropertyChangeSupport)object).getListeners()):null;
	}

	public synchronized PersistentPropertyChangeSupport getChild(String propertyName) {
		return ((PersistentPropertyChangeSupport)object).getChildren() != null && propertyName != null?(PersistentPropertyChangeSupport)((PersistentPropertyChangeSupport)object).getChildren().get(propertyName):null;
	}

	public synchronized boolean hasListeners(String propertyName) {
		if (((PersistentPropertyChangeSupport)object).getListeners() != null && !((PersistentPropertyChangeSupport)object).getListeners().isEmpty()) {
			// there is a generic listener
			return true;
		}
		if (((PersistentPropertyChangeSupport)object).getChildren() != null) {
			PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)((PersistentPropertyChangeSupport)object).getChildren().get(propertyName);
			if (child != null && child.getListeners() != null) {
				return !child.getListeners().isEmpty();
			}
		}
		return false;
	}
}
