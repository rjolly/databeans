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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
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

public class PersistentVetoableChangeSupport extends PersistentObject {
	protected Accessor accessor() throws RemoteException {
		return new VetoableChangeSupportAccessor(this);
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

	protected void init(Object sourceBean) {
		if (sourceBean == null) {
			throw new NullPointerException();
		}
		setSource(sourceBean);
	}

	public void addVetoableChangeListener(VetoableChangeListener listener) {
		execute(
			new MethodCall(this,"addVetoableChangeListener",new Class[] {VetoableChangeListener.class},new Object[] {listener}));
	}

	public void removeVetoableChangeListener(VetoableChangeListener listener) {
		execute(
			new MethodCall(this,"removeVetoableChangeListener",new Class[] {VetoableChangeListener.class},new Object[] {listener}));
	}

	public void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		execute(
			new MethodCall(this,"addVetoableChangeListener",new Class[] {String.class,VetoableChangeListener.class},new Object[] {propertyName,listener}));
	}

	public void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		execute(
			new MethodCall(this,"removeVetoableChangeListener",new Class[] {String.class,VetoableChangeListener.class},new Object[] {propertyName,listener}));
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

		Collection targets = getTargets();
		PersistentVetoableChangeSupport child = getChild(propertyName);

		if (targets != null) {
			try {
				Iterator t=targets.iterator();
				while(t.hasNext()) {
					VetoableChangeListener target = (VetoableChangeListener)t.next();
					target.vetoableChange(evt);
				}
			} catch (PropertyVetoException veto) {
				// Create an event to revert everyone to the old value.
	   			evt = new PropertyChangeEvent(getSource(), propertyName, newValue, oldValue);
				Iterator t=targets.iterator();
				while(t.hasNext()) {
					try {
						VetoableChangeListener target = (VetoableChangeListener)t.next();
						target.vetoableChange(evt);
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

	Collection getTargets() {
		return (Collection)execute(
			new MethodCall(this,"getTargets",new Class[] {},new Object[] {}));
	}

	PersistentVetoableChangeSupport getChild(String propertyName) {
		return (PersistentVetoableChangeSupport)execute(
			new MethodCall(this,"getChild",new Class[] {String.class},new Object[] {propertyName}));
	}

	public boolean hasListeners(String propertyName) {
		return ((Boolean)execute(
			new MethodCall(this,"hasListeners",new Class[] {String.class},new Object[] {propertyName}))).booleanValue();
	}
}

class VetoableChangeSupportAccessor extends Accessor {
	VetoableChangeSupportAccessor(PersistentObject object) throws RemoteException {
		super(object);
	}

	public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
		if (((PersistentVetoableChangeSupport)object).getListeners() == null) {
			((PersistentVetoableChangeSupport)object).setListeners((List)((PersistentVetoableChangeSupport)object).create(PersistentArrayList.class));
		}
		((PersistentVetoableChangeSupport)object).getListeners().add(listener);
	}

	public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
		if (((PersistentVetoableChangeSupport)object).getListeners() == null) {
			return;
		}
		((PersistentVetoableChangeSupport)object).getListeners().remove(listener);
	}

	public synchronized void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		if (((PersistentVetoableChangeSupport)object).getChildren() == null) {
			((PersistentVetoableChangeSupport)object).setChildren((Map)((PersistentVetoableChangeSupport)object).create(PersistentHashMap.class));
		}
		PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)((PersistentVetoableChangeSupport)object).getChildren().get(propertyName);
		if (child == null) {
			child = (PersistentVetoableChangeSupport)((PersistentVetoableChangeSupport)object).create(PersistentVetoableChangeSupport.class, new Class[] {Object.class}, new Object[] {((PersistentVetoableChangeSupport)object).getSource()});
			((PersistentVetoableChangeSupport)object).getChildren().put(propertyName, child);
		}
		child.addVetoableChangeListener(listener);
	}

	public synchronized void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
		if (((PersistentVetoableChangeSupport)object).getChildren() == null) {
			return;
		}
		PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)((PersistentVetoableChangeSupport)object).getChildren().get(propertyName);
		if (child == null) {
			return;
		}
		child.removeVetoableChangeListener(listener);
	}

	public synchronized Collection getTargets() {
		return ((PersistentVetoableChangeSupport)object).getListeners() != null?new ArrayList(((PersistentVetoableChangeSupport)object).getListeners()):null;
	}

	public synchronized PersistentVetoableChangeSupport getChild(String propertyName) {
		return ((PersistentVetoableChangeSupport)object).getChildren() != null && propertyName != null?(PersistentVetoableChangeSupport)((PersistentVetoableChangeSupport)object).getChildren().get(propertyName):null;
	}

	public synchronized boolean hasListeners(String propertyName) {
		if (((PersistentVetoableChangeSupport)object).getListeners() != null && !((PersistentVetoableChangeSupport)object).getListeners().isEmpty()) {
			// there is a generic listener
			return true;
		}
		if (((PersistentVetoableChangeSupport)object).getChildren() != null) {
			PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)((PersistentVetoableChangeSupport)object).getChildren().get(propertyName);
			if (child != null && child.getListeners() != null) {
				return !child.getListeners().isEmpty();
			}
		}
		return false;
	}
}
