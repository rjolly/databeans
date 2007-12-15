/*
 * @(#)AbstractList.java	1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

class SubList extends AbstractList {
	private List l;
	private int offset;
	private int size;
	private int expectedModCount;

	SubList(List list, int fromIndex, int toIndex) {
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > list.size())
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex +
											   ") > toIndex(" + toIndex + ")");
		l = list;
		offset = fromIndex;
		size = toIndex - fromIndex;
		expectedModCount = modCount(l);
	}

	static int modCount(List l) {
		return l instanceof PersistentAbstractList?((PersistentAbstractList)l).modCount():((AbstractList)l).modCount;
	}

	public Object set(int index, Object element) {
		rangeCheck(index);
		checkForComodification();
		return l.set(index+offset, element);
	}

	public Object get(int index) {
		rangeCheck(index);
		checkForComodification();
		return l.get(index+offset);
	}

	public int size() {
		checkForComodification();
		return size;
	}

	public void add(int index, Object element) {
		if (index<0 || index>size)
			throw new IndexOutOfBoundsException();
		checkForComodification();
		l.add(index+offset, element);
		expectedModCount = modCount(l);
		size++;
		modCount++;
	}

	public Object remove(int index) {
		rangeCheck(index);
		checkForComodification();
		Object result = l.remove(index+offset);
		expectedModCount = modCount(l);
		size--;
		modCount++;
		return result;
	}

	static void removeRange(List l, int fromIndex, int toIndex) {
		if(l instanceof PersistentAbstractList) ((PersistentAbstractList)l).removeRange(fromIndex,toIndex); else ((AbstractList)l).removeRange(fromIndex,toIndex);
	}

	protected void removeRange(int fromIndex, int toIndex) {
		checkForComodification();
		removeRange(l, fromIndex+offset, toIndex+offset);
		expectedModCount = modCount(l);
		size -= (toIndex-fromIndex);
		modCount++;
	}

	public boolean addAll(Collection c) {
		return addAll(size, c);
	}

	public boolean addAll(int index, Collection c) {
		if (index<0 || index>size)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+size);
		int cSize = c.size();
		if (cSize==0)
			return false;

		checkForComodification();
		l.addAll(offset+index, c);
		expectedModCount = modCount(l);
		size += cSize;
		modCount++;
		return true;
	}

	public Iterator iterator() {
		return listIterator();
	}

	public ListIterator listIterator(final int index) {
		checkForComodification();
		if (index<0 || index>size)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+size);

		return new ListIterator() {
			private ListIterator i = l.listIterator(index+offset);

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
			}
		};
	}

	public List subList(int fromIndex, int toIndex) {
		return new SubList(this, fromIndex, toIndex);
	}

	private void rangeCheck(int index) {
		if (index<0 || index>=size)
			throw new IndexOutOfBoundsException("Index: "+index+
												",Size: "+size);
	}

	private void checkForComodification() {
		if (modCount(l) != expectedModCount)
			throw new ConcurrentModificationException();
	}
}
