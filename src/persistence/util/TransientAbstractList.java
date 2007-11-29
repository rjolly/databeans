/*
 * @(#)AbstractList.java	1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

abstract class TransientAbstractList extends TransientAbstractCollection implements RemoteList {
	protected TransientAbstractList(Object mutex) throws RemoteException {
		super(mutex);
	}

	public boolean add(Object o) {
	synchronized(mutex()) {
		add(size(), o);
		return true;
	}
	}

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

	public int indexOf(Object o) {
	synchronized(mutex()) {
		ListIterator e = PersistentCollections.localList(this).listIterator();
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
	}

	public int lastIndexOf(Object o) {
	synchronized(mutex()) {
		ListIterator e = PersistentCollections.localList(this).listIterator(size());
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
	}

	public void clear() {
	synchronized(mutex()) {
		removeRange(0, size());
	}
	}

	public boolean addAll(int index, RemoteCollection c) {
	synchronized(mutex()) {
		boolean modified = false;
		Iterator e = PersistentCollections.localCollection(c).iterator();
		while (e.hasNext()) {
			add(index++, e.next());
			modified = true;
		}
		return modified;
	}
	}

	public RemoteIterator iterator() throws RemoteException {
		return new Itr();
	}

	public RemoteListIterator listIterator() throws RemoteException {
		return listIterator(0);
	}

	public RemoteListIterator listIterator(final int index) throws RemoteException {
		if (index<0 || index>size())
		  throw new IndexOutOfBoundsException("Index: "+index);

		return new ListItr(index);
	}

	private class Itr extends UnicastRemoteObject implements RemoteIterator {

		int cursor = 0;

		int lastRet = -1;

		int expectedModCount = modCount;

		Itr() throws RemoteException {}

		public boolean hasNext() {
			return cursor != size();
		}

		public Object next() {
			try {
				Object next = get(cursor);
				checkForComodification();
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
				TransientAbstractList.this.remove(lastRet);
				if (lastRet < cursor)
					cursor--;
				lastRet = -1;
				expectedModCount = modCount;
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		final void checkForComodification() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}
	}

	private class ListItr extends Itr implements RemoteListIterator {
		ListItr(int index) throws RemoteException {
			cursor = index;
		}

		public boolean hasPrevious() {
			return cursor != 0;
		}

		public Object previous() {
			try {
				Object previous = get(--cursor);
				checkForComodification();
				lastRet = cursor;
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
				TransientAbstractList.this.set(lastRet, o);
				expectedModCount = modCount;
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		public void add(Object o) {
			checkForComodification();

			try {
				TransientAbstractList.this.add(cursor++, o);
				lastRet = -1;
				expectedModCount = modCount;
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}
	}

	public RemoteList subList(int fromIndex, int toIndex) throws RemoteException {
	synchronized(mutex()) {
		return new SubList(this, fromIndex, toIndex, mutex());
	}
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof List))
			return false;

		ListIterator e1 = PersistentCollections.localList(this).listIterator();
		ListIterator e2 = PersistentCollections.localList((RemoteList) o).listIterator();
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
		Iterator i = PersistentCollections.localList(this).iterator();
	 		while (i.hasNext()) {
			Object obj = i.next();
			hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

	protected void removeRange(int fromIndex, int toIndex) {
		ListIterator it = PersistentCollections.localList(this).listIterator(fromIndex);
		for (int i=0, n=toIndex-fromIndex; i<n; i++) {
			it.next();
			it.remove();
		}
	}

	protected transient int modCount = 0;
}
