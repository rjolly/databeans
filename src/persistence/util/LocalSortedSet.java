/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.SortedSet;

class LocalSortedSet extends LocalSet implements SortedSet {
	RemoteSortedSet ss;

	LocalSortedSet(RemoteSortedSet s) {
		super(s);
		ss = s;
	}

	public Comparator comparator() {
		try {
			return ss.comparator();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public SortedSet subSet(Object fromElement, Object toElement) {
		try {
			return ss.subSet(fromElement, toElement);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public SortedSet headSet(Object toElement) {
		try {
			return ss.headSet(toElement);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public SortedSet tailSet(Object fromElement) {
		try {
			return ss.tailSet(fromElement);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object first() {
		try {
			return ss.first();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object last() {
		try {
			return ss.last();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
