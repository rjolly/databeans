/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

class LocalList extends LocalCollection implements List {
	RemoteList list;

	LocalList(RemoteList list) {
		super(list);
		this.list = list;
	}

	public Object get(int index) {
		try {
			return list.get(index);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object set(int index, Object element) {
		try {
			return list.set(index, element);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void add(int index, Object element) {
		try {
			list.add(index, element);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object remove(int index) {
		try {
			return list.remove(index);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public int indexOf(Object o) {
		try {
			return list.indexOf(o);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public int lastIndexOf(Object o) {
		try {
			return list.lastIndexOf(o);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean addAll(int index, Collection c) {
		try {
			return list.addAll(index, c);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public ListIterator listIterator() {
		try {
			return list.listIterator();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public ListIterator listIterator(int index) {
		try {
			return list.listIterator(index);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public List subList(int fromIndex, int toIndex) {
		try {
			return list.subList(fromIndex, toIndex);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
