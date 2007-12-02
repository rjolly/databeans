/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.SortedMap;

class LocalSortedMap extends LocalMap implements SortedMap {
	RemoteSortedMap sm;

	LocalSortedMap(RemoteSortedMap m) {
		super(m);
		sm = m;
	}

	public Comparator comparator() {
		try {
			return sm.comparator();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public SortedMap subMap(Object fromKey, Object toKey) {
		try {
			return sm.subMap(fromKey, toKey);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public SortedMap headMap(Object toKey) {
		try {
			return sm.headMap(toKey);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public SortedMap tailMap(Object fromKey) {
		try {
			return sm.tailMap(fromKey);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object firstKey() {
		try {
			return sm.firstKey();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object lastKey() {
		try {
			return sm.lastKey();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
