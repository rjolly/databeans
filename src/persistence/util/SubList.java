/*
 * @(#)AbstractList.java	1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import persistence.TransientObject;

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
	synchronized(mutex()) {
		rangeCheck(index);
		checkForComodification();
		return PersistentCollections.localList(l).set(index+offset, element);
	}
	}

	public Object get(int index) {
	synchronized(mutex()) {
		rangeCheck(index);
		checkForComodification();
		return PersistentCollections.localList(l).get(index+offset);
	}
	}

	public int size() {
	synchronized(mutex()) {
		checkForComodification();
		return size;
	}
	}

	public void add(int index, Object element) {
	synchronized(mutex()) {
		if (index<0 || index>size) throw new IndexOutOfBoundsException();
		checkForComodification();
		PersistentCollections.localList(l).add(index+offset, element);
		expectedModCount = modCount(l);
		size++;
		modCount++;
	}
	}

	public Object remove(int index) {
	synchronized(mutex()) {
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
	synchronized(mutex()) {
		return addAll(size, c);
	}
	}

	public boolean addAll(int index, RemoteCollection c) {
	synchronized(mutex()) {
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

	private class ListItr extends TransientObject implements RemoteListIterator {
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
	synchronized(mutex()) {
		return new SubList(this, fromIndex, toIndex, mutex());
	}
	}

	private void rangeCheck(int index) {
		if (index<0 || index>=size) throw new IndexOutOfBoundsException("Index: "+index+",Size: "+size);
	}

	private void checkForComodification() {
		if (modCount(l) != expectedModCount) throw new ConcurrentModificationException();
	}
}
