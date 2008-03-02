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

public class SubList extends AbstractList {
	public AbstractList getL() {
		return (AbstractList)get("l");
	}

	public void setL(AbstractList list) {
		set("l",list);
	}

	public int getOffset() {
		return ((Integer)get("offset")).intValue();
	}

	public void setOffset(int n) {
		set("offset",new Integer(n));
	}

	public int getSize() {
		return ((Integer)get("size")).intValue();
	}

	public void setSize(int n) {
		set("size",new Integer(n));
	}

	public int getExpectedModCount() {
		return ((Integer)get("expectedModCount")).intValue();
	}

	public void setExpectedModCount(int n) {
		set("expectedModCount",new Integer(n));
	}

	public void init(AbstractList list, int fromIndex, int toIndex) {
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > list.size())
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex +
							") > toIndex(" + toIndex + ")");
		setL(list);
		setOffset(fromIndex);
		setSize(toIndex - fromIndex);
		setExpectedModCount(getL().modCount());
	}

	public void init(AbstractList list, int offset, int fromIndex, int toIndex, int size) {
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > size)
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex +
							") > toIndex(" + toIndex + ")");
		setL(list);
		setOffset(offset+fromIndex);
		setSize(toIndex - fromIndex);
		setExpectedModCount(getL().modCount());
	}

	public Object set(int index, Object element) {
		rangeCheck(index);
		checkForComodification();
		return getL().set(index+getOffset(), element);
	}

	public Object get(int index) {
		rangeCheck(index);
		checkForComodification();
		return getL().get(index+getOffset());
	}

	public int size() {
		checkForComodification();
		return getSize();
	}

	public void add(int index, Object element) {
		if (index<0 || index>getSize())
			throw new IndexOutOfBoundsException();
		checkForComodification();
		getL().add(index+getOffset(), element);
		setExpectedModCount(getL().modCount());
		setSize(getSize()+1);
		setModCount(getModCount()+1);
	}

	public Object remove(int index) {
		rangeCheck(index);
		checkForComodification();
		Object result = getL().remove(index+getOffset());
		setExpectedModCount(getL().modCount());
		setSize(getSize()-1);
		setModCount(getModCount()+1);
		return result;
	}

	protected void removeRange(int fromIndex, int toIndex) {
		checkForComodification();
		getL().removeRange(fromIndex+getOffset(), toIndex+getOffset());
		setExpectedModCount(getL().modCount());
		setSize(getSize()-(toIndex-fromIndex));
		setModCount(getModCount()+1);
	}

	public boolean addAll(Collection c) {
		return addAll(getSize(), c);
	}

	public boolean addAll(int index, Collection c) {
		if (index<0 || index>getSize())
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());
		int cSize = c.size();
		if (cSize==0)
			return false;

		checkForComodification();
		getL().addAll(getOffset()+index, c);
		setExpectedModCount(getL().modCount());
		setSize(getSize()+cSize);
		setModCount(getModCount()+1);
		return true;
	}

	public Iterator iterator() {
		return listIterator();
	}

	public ListIterator listIterator(final int index) {
		checkForComodification();
		if (index<0 || index>getSize())
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());

		return new ListIterator() {
			private ListIterator i = getL().listIterator(index+getOffset());

			public boolean hasNext() {
				return nextIndex() < getSize();
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
				return i.nextIndex() - getOffset();
			}

			public int previousIndex() {
				return i.previousIndex() - getOffset();
			}

			public void remove() {
				i.remove();
				setExpectedModCount(getL().modCount());
				setSize(getSize()-1);
				setModCount(getModCount()+1);
			}

			public void set(Object o) {
				i.set(o);
			}

			public void add(Object o) {
				i.add(o);
				setExpectedModCount(getL().modCount());
				setSize(getSize()+1);
				setModCount(getModCount()+1);
			}
		};
	}

	public List subList(int fromIndex, int toIndex) {
		return (List)create(SubList.class,new Class[] {AbstractList.class,int.class,int.class,int.class,int.class},new Object[] {getL(),new Integer(getOffset()),new Integer(fromIndex),new Integer(toIndex),new Integer(getSize())});
	}

	private void rangeCheck(int index) {
		if (index<0 || index>=getSize())
			throw new IndexOutOfBoundsException("Index: "+index+
								",Size: "+getSize());
	}

	private void checkForComodification() {
		if (getL().modCount() != getExpectedModCount())
			throw new ConcurrentModificationException();
	}
}
