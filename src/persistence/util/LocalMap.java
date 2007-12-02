/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import persistence.LocalWrapper;

class LocalMap extends LocalWrapper implements Map {
	RemoteMap m;				// Backing Map

	LocalMap(RemoteMap m) {
		super(m);
		this.m = m;
	}

	public int size() {
		try {
			return m.size();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isEmpty() {
		try {
			return m.isEmpty();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean containsKey(Object key) {
		try {
			return m.containsKey(key);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean containsValue(Object value) {
		try {
			return m.containsValue(value);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object get(Object key) {
		try {
			return m.get(key);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object put(Object key, Object value) {
		try {
			return m.put(key, value);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object remove(Object key) {
		try {
			return m.remove(key);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void putAll(Map map) {
		try {
			m.putAll(map);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void clear() {
		try {
			m.clear();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	private transient Set keySet = null;
	private transient Set entrySet = null;
	private transient Collection values = null;

	public Set keySet() {
		try {
			if (keySet==null)
				keySet = m.keySet();
			return keySet;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Set entrySet() {
		try {
			if (entrySet==null)
				entrySet = m.entrySet();
			return entrySet;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Collection values() {
		try {
			if (values==null)
				values = m.values();
			return values;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
