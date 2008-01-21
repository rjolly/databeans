/*
 * @(#)AbstractCollection.java		1.24 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import persistence.PersistentObject;

public abstract class PersistentAbstractCollection extends PersistentObject implements Collection {
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		public synchronized boolean add(Object o, boolean b) {
			return b?add0(o):false;
		}

		boolean add0(Object o) {
			throw new UnsupportedOperationException();
		}

		public synchronized boolean remove(Object o, boolean b) {
			return b?remove0(o):false;
		}

		boolean remove0(Object o) {
			Iterator e = iterator();
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

		//  String conversion

		public synchronized String persistentToString() {
			StringBuffer buf = new StringBuffer();
			buf.append("[");

			Iterator i = iterator();
			boolean hasNext = i.hasNext();
			while (hasNext) {
				Object o = i.next();
				buf.append(o == PersistentAbstractCollection.this ? "(this Collection)" : String.valueOf(o));
				hasNext = i.hasNext();
				if (hasNext)
					buf.append(", ");
			}

			buf.append("]");
			return buf.toString();
		}
	}

	// Query Operations

	public abstract Iterator iterator();

	public abstract int size();

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean contains(Object o) {
		Iterator e = iterator();
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

	public Object[] toArray() {
		Object[] result = new Object[size()];
		Iterator e = iterator();
		for (int i=0; e.hasNext(); i++)
			result[i] = e.next();
		return result;
	}

	public Object[] toArray(Object a[]) {
		int size = size();
		if (a.length < size)
			a = (Object[])java.lang.reflect.Array.newInstance(
								  a.getClass().getComponentType(), size);

		Iterator it=iterator();
		for (int i=0; i<size; i++)
			a[i] = it.next();

		if (a.length > size)
			a[size] = null;

		return a;
	}

	// Modification Operations

	public boolean add(Object o) {
		return ((Boolean)execute(
			new MethodCall("add",new Class[] {Object.class,boolean.class},new Object[] {o,new Boolean(true)}),
			new MethodCall("remove",new Class[] {Object.class,boolean.class},new Object[] {o,null}),1)).booleanValue();
	}

	public boolean remove(Object o) {
		return ((Boolean)execute(
			new MethodCall("remove",new Class[] {Object.class,boolean.class},new Object[] {o,new Boolean(true)}),
			new MethodCall("add",new Class[] {Object.class,boolean.class},new Object[] {o,null}),1)).booleanValue();
	}

	// Bulk Operations

	public boolean containsAll(Collection c) {
		Iterator e = c.iterator();
		while (e.hasNext())
			if(!contains(e.next()))
				return false;

		return true;
	}

	public boolean addAll(Collection c) {
		boolean modified = false;
		Iterator e = c.iterator();
		while (e.hasNext()) {
			if(add(e.next()))
				modified = true;
		}
		return modified;
	}

	public boolean removeAll(Collection c) {
		boolean modified = false;
		Iterator e = iterator();
		while (e.hasNext()) {
			if(c.contains(e.next())) {
				e.remove();
				modified = true;
			}
		}
		return modified;
	}

	public boolean retainAll(Collection c) {
		boolean modified = false;
		Iterator e = iterator();
		while (e.hasNext()) {
			if(!c.contains(e.next())) {
				e.remove();
				modified = true;
			}
		}
		return modified;
	}

	public void clear() {
		Iterator e = iterator();
		while (e.hasNext()) {
			e.next();
			e.remove();
		}
	}
}
