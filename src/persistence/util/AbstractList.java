/*
 * @(#)AbstractList.java		1.37 03/01/18
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
import java.util.RandomAccess;
import persistence.PersistentObject;
import persistence.Store;

public abstract class AbstractList extends AbstractCollection implements List {
	public AbstractList() {
	}

	public AbstractList(final Store store) {
		super(store);
	}

	public boolean add(Object o) {
		add(size(), o);
		return true;
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

	int modCount() {
		return getModCount();
	}

	private class Itr implements Iterator {
		int cursor = 0;
		int lastRet = -1;
		int expectedModCount = modCount();

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
				AbstractList.this.remove(lastRet);
				if (lastRet < cursor)
					cursor--;
				lastRet = -1;
				expectedModCount = modCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		final void checkForComodification() {
			if (modCount() != expectedModCount)
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
				AbstractList.this.set(lastRet, o);
				expectedModCount = modCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		public void add(Object o) {
			checkForComodification();

			try {
				AbstractList.this.add(cursor++, o);
				lastRet = -1;
				expectedModCount = modCount();
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

	// Comparison and hashing

	public synchronized boolean equals(PersistentObject o) {
		if (o == AbstractList.this)
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

	public synchronized int hashCode() {
		int hashCode = 1;
		Iterator i = iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
		}
		return hashCode;
	}
}

class SubList extends java.util.AbstractList {
	protected AbstractList l;
	protected int offset;
	private int size;
	private int expectedModCount;

	SubList(AbstractList list, int fromIndex, int toIndex) {
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
		expectedModCount = l.modCount();
	}

	SubList(AbstractList list, int offset, int fromIndex, int toIndex, int size) {
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > size)
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex +
			") > toIndex(" + toIndex + ")");
		l = list;
		offset = offset+fromIndex;
		size = toIndex - fromIndex;
		expectedModCount = l.modCount();
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
		expectedModCount = l.modCount();
		size++;
	}

	public Object remove(int index) {
		rangeCheck(index);
		checkForComodification();
		Object result = l.remove(index+offset);
		expectedModCount = l.modCount();
		size--;
		return result;
	}

	protected void removeRange(int fromIndex, int toIndex) {
		checkForComodification();
		l.removeRange(fromIndex+offset, toIndex+offset);
		expectedModCount = l.modCount();
		size -= (toIndex-fromIndex);
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
		expectedModCount = l.modCount();
		size += cSize;
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
				expectedModCount = l.modCount();
				size--;
			}

			public void set(Object o) {
				i.set(o);
			}

			public void add(Object o) {
				i.add(o);
				expectedModCount = l.modCount();
				size++;
			}
		};
	}

	public List subList(int fromIndex, int toIndex) {
		return new SubList(l, offset, fromIndex, toIndex, size);
	}

	private void rangeCheck(int index) {
		if (index<0 || index>=size)
			throw new IndexOutOfBoundsException("Index: "+index+
			",Size: "+size);
	}

	private void checkForComodification() {
		if (l.modCount() != expectedModCount)
			throw new ConcurrentModificationException();
	}
}

class RandomAccessSubList extends SubList implements RandomAccess {
	RandomAccessSubList(AbstractList list, int fromIndex, int toIndex) {
		super(list, fromIndex, toIndex);
	}

	public List subList(int fromIndex, int toIndex) {
		return new RandomAccessSubList(l, offset+fromIndex, offset+toIndex);
	}
}
