/*
 * @(#)AbstractList.java		1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

public abstract class PersistentAbstractList extends PersistentAbstractCollection implements List {
	protected abstract class Accessor extends PersistentAbstractCollection.Accessor {
		public Accessor() throws RemoteException {}

		abstract public Object get(int index);

		public Object set(int index, Object element) {
			throw new UnsupportedOperationException();
		}

		public void add(int index, Object element) {
			throw new UnsupportedOperationException();
		}

		public Object remove(int index) {
			throw new UnsupportedOperationException();
		}
	}

	public boolean add(Object o) {
		add(size(), o);
		return true;
	}

	public Object get(int index) {
		return execute(
			new MethodCall("get",new Class[] {int.class},new Object[] {new Integer(index)}));
	}

	public Object set(int index, Object element) {
		return execute(
			new MethodCall("set",new Class[] {int.class,Object.class},new Object[] {new Integer(index),element}),
			new MethodCall("set",new Class[] {int.class,Object.class},new Object[] {new Integer(index),null}),1);
	}

	public void add(int index, Object element) {
		execute(
			new MethodCall("add",new Class[] {int.class,Object.class},new Object[] {new Integer(index),element}),
			new MethodCall("remove",new Class[] {int.class},new Object[] {null}),0);
	}

	public Object remove(int index) {
		return execute(
			new MethodCall("remove",new Class[] {int.class},new Object[] {new Integer(index)}),
			new MethodCall("add",new Class[] {int.class,Object.class},new Object[] {new Integer(index),null}),1);
	}

	// Search Operations

	public int indexOf(Object o) {
		ListIterator e = listIterator();
		if (o==null) {
			while (e.hasNext())
				if (e.next()==null)
					return e.previousIndex();
		} else {
			while (e.hasNext())
				if (o.equals(e.next()))
					return e.previousIndex();
		}
		return -1;
	}

	public int lastIndexOf(Object o) {
		ListIterator e = listIterator(size());
		if (o==null) {
			while (e.hasPrevious())
				if (e.previous()==null)
					return e.nextIndex();
		} else {
			while (e.hasPrevious())
				if (o.equals(e.previous()))
					return e.nextIndex();
		}
		return -1;
	}

	// Bulk Operations

	public void clear() {
		removeRange(0, size());
	}

	public boolean addAll(int index, Collection c) {
		boolean modified = false;
		Iterator e = c.iterator();
		while (e.hasNext()) {
			add(index++, e.next());
			modified = true;
		}
		return modified;
	}

	// Iterators

	public Iterator iterator() {
		return new Itr();
	}

	public ListIterator listIterator() {
		return listIterator(0);
	}

	public ListIterator listIterator(final int index) {
		if (index<0 || index>size())
		  throw new IndexOutOfBoundsException("Index: "+index);

		return new ListItr(index);
	}

	private class Itr implements Iterator {
		int cursor = 0;
		int lastRet = -1;
		int expectedModCount = getModCount();

		public boolean hasNext() {
			return cursor != size();
		}

		public Object next() {
			checkForComodification();
			try {
				Object next = get(cursor);
				lastRet = cursor++;
				return next;
			} catch(IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			if (lastRet == -1)
				throw new IllegalStateException();
			checkForComodification();

			try {
				PersistentAbstractList.this.remove(lastRet);
				if (lastRet < cursor)
					cursor--;
				lastRet = -1;
				expectedModCount = getModCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		final void checkForComodification() {
			if (getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
		}
	}

	private class ListItr extends Itr implements ListIterator {
		ListItr(int index) {
			cursor = index;
		}

		public boolean hasPrevious() {
			return cursor != 0;
		}

		public Object previous() {
			checkForComodification();
			try {
				int i = cursor - 1;
				Object previous = get(i);
				lastRet = cursor = i;
				return previous;
			} catch(IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		public int nextIndex() {
			return cursor;
		}

		public int previousIndex() {
			return cursor-1;
		}

		public void set(Object o) {
			if (lastRet == -1)
				throw new IllegalStateException();
			checkForComodification();

			try {
				PersistentAbstractList.this.set(lastRet, o);
				expectedModCount = getModCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		public void add(Object o) {
			checkForComodification();

			try {
				PersistentAbstractList.this.add(cursor++, o);
				lastRet = -1;
				expectedModCount = getModCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}
	}

	public List subList(int fromIndex, int toIndex) {
		return (this instanceof RandomAccess ?
				new RandomAccessSubList(this, fromIndex, toIndex) :
				new SubList(this, fromIndex, toIndex));
	}

	// Comparison and hashing

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof List))
			return false;

		ListIterator e1 = listIterator();
		ListIterator e2 = ((List) o).listIterator();
		while(e1.hasNext() && e2.hasNext()) {
			Object o1 = e1.next();
			Object o2 = e2.next();
			if (!(o1==null ? o2==null : o1.equals(o2)))
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());
	}

	public int hashCode() {
		int hashCode = 1;
		Iterator i = iterator();
	 		while (i.hasNext()) {
			Object obj = i.next();
			hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

	protected void removeRange(int fromIndex, int toIndex) {
		ListIterator it = listIterator(fromIndex);
		for (int i=0, n=toIndex-fromIndex; i<n; i++) {
			it.next();
			it.remove();
		}
	}

	public int getModCount() {
		return ((Integer)get("modCount")).intValue();
	}

	public void setModCount(int n) {
		set("modCount",new Integer(n));
	}
}
