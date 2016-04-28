/*
 * @(#)AbstractCollection.java		1.24 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Iterator;
import persistence.PersistentObject;
import persistence.Secondary;
import persistence.Store;

public abstract class AbstractCollection<E> extends PersistentObject implements Collection<E> {
	public AbstractCollection() {
	}

	public AbstractCollection(final Store store) {
		super(store);
	}

	// Query Operations

	public abstract Iterator<E> iterator();

	public abstract int size();

	@Secondary
	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean contains(Object o) {
		Iterator<E> e = iterator();
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
		Iterator<E> e = iterator();
		for (int i=0; e.hasNext(); i++)
			result[i] = e.next();
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T a[]) {
		int size = size();
		if (a.length < size)
			a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

		Iterator<E> it=iterator();
		for (int i=0; i<size; i++)
			a[i] = (T)it.next();

		if (a.length > size)
			a[size] = null;

		return a;
	}

	// Modification Operations

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public synchronized boolean remove(Object o) {
		Iterator<E> e = iterator();
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

	// Bulk Operations

	public boolean containsAll(Collection<?> c) {
		Iterator<?> e = c.iterator();
		while (e.hasNext())
			if(!contains(e.next()))
				return false;

		return true;
	}

	public boolean addAll(Collection<? extends E> c) {
		boolean modified = false;
		Iterator<? extends E> e = c.iterator();
		while (e.hasNext()) {
			if(add(e.next()))
				modified = true;
		}
		return modified;
	}

	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		Iterator<?> e = iterator();
		while (e.hasNext()) {
			if(c.contains(e.next())) {
				e.remove();
				modified = true;
			}
		}
		return modified;
	}

	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		Iterator<E> e = iterator();
		while (e.hasNext()) {
			if(!c.contains(e.next())) {
				e.remove();
				modified = true;
			}
		}
		return modified;
	}

	public void clear() {
		Iterator<E> e = iterator();
		while (e.hasNext()) {
			e.next();
			e.remove();
		}
	}

	//  String conversion

	public synchronized String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");

		Iterator<E> i = iterator();
		boolean hasNext = i.hasNext();
		while (hasNext) {
			Object o = i.next();
			buf.append(o == AbstractCollection.this ? "(this Collection)" : String.valueOf(o));
			hasNext = i.hasNext();
			if (hasNext)
				buf.append(", ");
		}

		buf.append("]");
		return buf.toString();
	}
}
