/*
 * @(#)AbstractList.java	1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import persistence.*;

public abstract class PersistentAbstractList extends PersistentAbstractCollection implements RemoteList {
	public PersistentAbstractList() throws RemoteException {}

	public PersistentAbstractList(Object mutex) throws RemoteException {
		super(mutex);
	}

	public PersistentAbstractList(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
		mutex=this;
	}

	public boolean add(Object o) {
	synchronized(mutex) {
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
	synchronized(mutex) {
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
	synchronized(mutex) {
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
	synchronized(mutex) {
		removeRange(0, size());
	}
	}

	public boolean addAll(int index, RemoteCollection c) {
	synchronized(mutex) {
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

		int expectedModCount = getModCount();

		Itr() throws RemoteException {}

		public boolean hasNext() {
			return cursor != size();
		}

		public Object next() {
			try {
				Object next = PersistentAbstractList.this.get(cursor);
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

	private class ListItr extends Itr implements RemoteListIterator {
		ListItr(int index) throws RemoteException {
			cursor = index;
		}

		public boolean hasPrevious() {
			return cursor != 0;
		}

		public Object previous() {
			try {
				Object previous = PersistentAbstractList.this.get(--cursor);
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

	public RemoteList subList(int fromIndex, int toIndex) throws RemoteException {
	synchronized(mutex) {
		return new SubList(this, fromIndex, toIndex, mutex);
	}
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RemoteList))
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

	public int getModCount() {
		return ((Integer)get("modCount")).intValue();
	}

	public void setModCount(int n) {
		set("modCount",new Integer(n));
	}
}

class SubList extends TransientAbstractList implements RemoteList {
	private RemoteList l;
	private int offset;
	private int size;
	private int expectedModCount;

	SubList(RemoteList list, int fromIndex, int toIndex, Object mutex) throws RemoteException {
		super(mutex);
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > list.size())
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
		l = list;
		offset = fromIndex;
		size = toIndex - fromIndex;
		expectedModCount = modCount(l);
	}

	int modCount(RemoteList l) {
		return l instanceof PersistentAbstractList?((PersistentAbstractList)l).getModCount():((TransientAbstractList)l).modCount;
	}

	public Object set(int index, Object element) {
	synchronized(mutex) {
		rangeCheck(index);
		checkForComodification();
		return PersistentCollections.localList(l).set(index+offset, element);
	}
	}

	public Object get(int index) {
	synchronized(mutex) {
		rangeCheck(index);
		checkForComodification();
		return PersistentCollections.localList(l).get(index+offset);
	}
	}

	public int size() {
	synchronized(mutex) {
		checkForComodification();
		return size;
	}
	}

	public void add(int index, Object element) {
	synchronized(mutex) {
		if (index<0 || index>size) throw new IndexOutOfBoundsException();
		checkForComodification();
		PersistentCollections.localList(l).add(index+offset, element);
		expectedModCount = modCount(l);
		size++;
		modCount++;
	}
	}

	public Object remove(int index) {
	synchronized(mutex) {
		rangeCheck(index);
		checkForComodification();
		Object result = PersistentCollections.localList(l).remove(index+offset);
		expectedModCount = modCount(l);
		size--;
		modCount++;
		return result;
	}
	}

	void removeRange(RemoteList l, int fromIndex, int toIndex) {
		if(l instanceof PersistentAbstractList) ((PersistentAbstractList)l).removeRange(fromIndex,toIndex);
		else ((TransientAbstractList)l).removeRange(fromIndex,toIndex);
	}

	protected void removeRange(int fromIndex, int toIndex) {
		checkForComodification();
		removeRange(l, fromIndex+offset, toIndex+offset);
		expectedModCount = modCount(l);
		size -= (toIndex-fromIndex);
		modCount++;
	}

	public boolean addAll(RemoteCollection c) {
	synchronized(mutex) {
		return addAll(size, c);
	}
	}

	public boolean addAll(int index, RemoteCollection c) {
	synchronized(mutex) {
		if (index<0 || index>size) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
		int cSize = PersistentCollections.localCollection(c).size();
		if (cSize==0)
			return false;

		checkForComodification();
		PersistentCollections.localList(l).addAll(offset+index, PersistentCollections.localCollection(c));
		expectedModCount = modCount(l);
		size += cSize;
		modCount++;
		return true;
	}
	}

	public RemoteIterator iterator() throws RemoteException {
		return listIterator();
	}

	public RemoteListIterator listIterator(final int index) throws RemoteException {
		checkForComodification();
		if (index<0 || index>size)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+size);

		return new ListItr(index);
	}

	private class ListItr extends UnicastRemoteObject implements RemoteListIterator {
		private ListIterator i;

		ListItr(int index) throws RemoteException {
			i = PersistentCollections.localList(l).listIterator(index+offset);
		}

		public boolean hasNext() {
			return nextIndex() < size;
		}

		public Object next() {
			if (hasNext())
				return i.next();
			else
				throw new NoSuchElementException();
		}

		public boolean hasPrevious() {
			return previousIndex() >= 0;
		}

		public Object previous() {
			if (hasPrevious())
				return i.previous();
			else
				throw new NoSuchElementException();
		}

		public int nextIndex() {
			return i.nextIndex() - offset;
		}

		public int previousIndex() {
			return i.previousIndex() - offset;
		}

		public void remove() {
			i.remove();
			expectedModCount = modCount(l);
			size--;
			modCount++;
		}

		public void set(Object o) {
			i.set(o);
		}

		public void add(Object o) {
			i.add(o);
			expectedModCount = modCount(l);
			size++;
			modCount++;
		};
	}

	public RemoteList subList(int fromIndex, int toIndex) throws RemoteException {
	synchronized(mutex) {
		return new SubList(this, fromIndex, toIndex, mutex);
	}
	}

	private void rangeCheck(int index) {
		if (index<0 || index>=size) throw new IndexOutOfBoundsException("Index: "+index+",Size: "+size);
	}

	private void checkForComodification() {
		if (modCount(l) != expectedModCount) throw new ConcurrentModificationException();
	}
}
