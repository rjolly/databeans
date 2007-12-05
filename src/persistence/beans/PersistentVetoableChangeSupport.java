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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import persistence.MethodCall;
import persistence.PersistentObject;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentHashMap;

public class PersistentVetoableChangeSupport extends PersistentObject {
	protected Accessor createAccessor() {
		return new Accessor() {
			public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
				if (getListeners() == null) {
					setListeners((List)create(PersistentArrayList.class));
				}
				getListeners().add(listener);
			}

			public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
				if (getListeners() == null) {
					return;
				}
				getListeners().remove(listener);
			}

			public synchronized void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
				if (getChildren() == null) {
					setChildren((Map)create(PersistentHashMap.class));
				}
				PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)getChildren().get(propertyName);
				if (child == null) {
					child = (PersistentVetoableChangeSupport)create(PersistentVetoableChangeSupport.class, new Class[] {Object.class}, new Object[] {getSource()});
					getChildren().put(propertyName, child);
				}
				child.addVetoableChangeListener(listener);
			}

			public synchronized void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
				if (getChildren() == null) {
					return;
				}
				PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)getChildren().get(propertyName);
				if (child == null) {
					return;
				}
				child.removeVetoableChangeListener(listener);
			}
		};
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

	public void init(Object sourceBean) {
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

		Collection targets = null;
		PersistentVetoableChangeSupport child = null;
		{
			if (getListeners() != null) {
				targets = new ArrayList(getListeners());
			}
			if (getChildren() != null && propertyName != null) {
				child = (PersistentVetoableChangeSupport)getChildren().get(propertyName);
			}
		}

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

	public boolean hasListeners(String propertyName) {
		if (getListeners() != null && !getListeners().isEmpty()) {
			// there is a generic listener
			return true;
		}
		if (getChildren() != null) {
			PersistentVetoableChangeSupport child = (PersistentVetoableChangeSupport)getChildren().get(propertyName);
			if (child != null && child.getListeners() != null) {
				return !child.getListeners().isEmpty();
			}
		}
		return false;
	}
}
