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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import persistence.MethodCall;
import persistence.PersistentObject;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentHashMap;

public class PersistentPropertyChangeSupport extends PersistentObject {
	protected Accessor createAccessor() {
		return new Accessor() {
			public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
				if (getListeners() == null) {
					setListeners((List)create(PersistentArrayList.class));
				}
				getListeners().add(listener);
			}

			public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
				if (getListeners() == null) {
					return;
				}
				getListeners().remove(listener);
			}

			public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
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

			public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
				if (getChildren() == null) {
					return;
				}
				PersistentPropertyChangeSupport child = (PersistentPropertyChangeSupport)getChildren().get(propertyName);
				if (child == null) {
					return;
				}
				child.removePropertyChangeListener(listener);
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

		Collection targets = null;
		PersistentPropertyChangeSupport child = null;
		{
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
				PropertyChangeListener target = (PropertyChangeListener)t.next();
				target.propertyChange(evt);
			}
		}

		if (child != null) {
			child.firePropertyChange(evt);
		}		
	}

	public boolean hasListeners(String propertyName) {
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
