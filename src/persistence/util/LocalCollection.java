/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import persistence.LocalWrapper;

class LocalCollection extends LocalWrapper implements Collection {
	RemoteCollection c;		   // Backing Collection

	LocalCollection(RemoteCollection c) {
		super(c);
		this.c = c;
	}

	public int size() {
		try {
			return c.size();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isEmpty() {
		try {
			return c.isEmpty();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean contains(Object o) {
		try {
			return c.contains(o);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object[] toArray() {
		try {
			return c.toArray();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object[] toArray(Object[] a) {
		try {
			return c.toArray(a);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterator iterator() {
		try {
			return c.iterator();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean add(Object o) {
		try {
			return c.add(o);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean remove(Object o) {
		try {
			return c.remove(o);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean containsAll(Collection coll) {
		try {
			return c.containsAll(coll);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean addAll(Collection coll) {
		try {
			return c.addAll(coll);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean removeAll(Collection coll) {
		try {
			return c.removeAll(coll);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean retainAll(Collection coll) {
		try {
			return c.retainAll(coll);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void clear() {
		try {
			c.clear();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
