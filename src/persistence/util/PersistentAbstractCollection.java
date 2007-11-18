/*
 * @(#)AbstractCollection.java	1.24 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.*;
import java.rmi.*;
import persistence.*;

public abstract class PersistentAbstractCollection extends PersistentObject implements RemoteCollection {
	Object mutex;

	public PersistentAbstractCollection() throws RemoteException {
		mutex=this;
	}

	public PersistentAbstractCollection(Object mutex) throws RemoteException {
		this.mutex=mutex;
	}

	public PersistentAbstractCollection(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
		mutex=this;
	}

	public abstract RemoteIterator iterator() throws RemoteException;

	public abstract int size();

	public boolean isEmpty() {
	synchronized(mutex) {
		return size() == 0;
	}
	}

	public boolean contains(Object o) {
	synchronized(mutex) {
		Iterator e = PersistentCollections.localCollection(this).iterator();
		if (o==null) {
			while (e.hasNext())
				if (e.next()==null)
					return true;
		} else {
			while (e.hasNext())
				if (o.equals(e.next()))
					return true;
		}
		return false;
	}
	}

	public Object[] toArray() {
	synchronized(mutex) {
		Object[] result = new Object[size()];
		Iterator e = PersistentCollections.localCollection(this).iterator();
		for (int i=0; e.hasNext(); i++)
			result[i] = e.next();
		return result;
	}
	}

	public Object[] toArray(Object a[]) {
	synchronized(mutex) {
		int size = size();
		if (a.length < size)
			a = (Object[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

		Iterator it=PersistentCollections.localCollection(this).iterator();
		for (int i=0; i<size; i++)
			a[i] = it.next();

		if (a.length > size)
			a[size] = null;

		return a;
	}
	}

	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object o) {
	synchronized(mutex) {
		Iterator e = PersistentCollections.localCollection(this).iterator();
		if (o==null) {
			while (e.hasNext()) {
				if (e.next()==null) {
					e.remove();
					return true;
				}
			}
		} else {
			while (e.hasNext()) {
				if (o.equals(e.next())) {
					e.remove();
					return true;
				}
			}
		}
		return false;
	}
	}

	public boolean containsAll(RemoteCollection c) {
	synchronized(mutex) {
		Iterator e = PersistentCollections.localCollection(c).iterator();
		while (e.hasNext())
			if(!contains(e.next()))
				return false;

		return true;
	}
	}

	public boolean addAll(RemoteCollection c) {
	synchronized(mutex) {
		boolean modified = false;
		Iterator e = PersistentCollections.localCollection(c).iterator();
		while (e.hasNext()) {
			if(add(e.next()))
				modified = true;
		}
		return modified;
	}
	}

	public boolean removeAll(RemoteCollection c) {
	synchronized(mutex) {
		boolean modified = false;
		Iterator e = PersistentCollections.localCollection(this).iterator();
		while (e.hasNext()) {
			if(PersistentCollections.localCollection(c).contains(e.next())) {
				e.remove();
				modified = true;
			}
		}
		return modified;
	}
	}

	public boolean retainAll(RemoteCollection c) {
	synchronized(mutex) {
		boolean modified = false;
		Iterator e = PersistentCollections.localCollection(this).iterator();
		while (e.hasNext()) {
			if(!PersistentCollections.localCollection(c).contains(e.next())) {
				e.remove();
				modified = true;
			}
		}
		return modified;
	}
	}

	public void clear() {
	synchronized(mutex) {
		Iterator e = PersistentCollections.localCollection(this).iterator();
		while (e.hasNext()) {
			e.next();
			e.remove();
		}
	}
	}

	public String remoteToString() {
	synchronized(mutex) {
		StringBuffer buf = new StringBuffer();
		Iterator e = PersistentCollections.localCollection(this).iterator();
		buf.append("[");
		int maxIndex = size() - 1;
		for (int i = 0; i <= maxIndex; i++) {
			buf.append(String.valueOf(e.next()));
			if (i < maxIndex)
				buf.append(", ");
		}
		buf.append("]");
		return buf.toString();
	}
	}
}
