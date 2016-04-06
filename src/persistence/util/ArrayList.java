/*
 * @(#)ArrayList.java		1.43 04/12/09
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import persistence.Array;
import persistence.PersistentArray;
import persistence.PersistentObject;
import persistence.Store;

public class ArrayList extends AbstractList implements List, RandomAccess, Cloneable {
	public ArrayList() {
	}

	public ArrayList(final Store store, int initialCapacity) {
		super(store);
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: "+
				initialCapacity);
		setElementData(new PersistentArray(store, Object.class, initialCapacity));
	}

	public ArrayList(final Store store) {
		this(store, 10);
	}

	public ArrayList(final Store store, Collection c) {
		super(store);
		setSize(c.size());
		// Allow 10% room for growth
		Object elementData[] = new Object[
			(int)Math.min((getSize()*110L)/100,Integer.MAX_VALUE)];
		c.toArray(elementData);
		setElementData(new PersistentArray(store, elementData));
	}

	public Array getElementData() {
		return (Array)get("elementData");
	}

	public void setElementData(Array array) {
		set("elementData",array);
	}

	public int getSize() {
		return ((Integer)get("size")).intValue();
	}

	public void setSize(int n) {
		set("size",new Integer(n));
	}

	public synchronized void trimToSize() {
		setModCount(getModCount()+1);
		int oldCapacity = getElementData().length();
		if (getSize() < oldCapacity) {
			Array oldData = getElementData();
			setElementData(new PersistentArray(getStore(), Object.class, getSize()));
			PersistentArray.copy(oldData, 0, getElementData(), 0, getSize());
		}
	}

	public synchronized void ensureCapacity(int minCapacity) {
		setModCount(getModCount()+1);
		int oldCapacity = getElementData().length();
		if (minCapacity > oldCapacity) {
			Array oldData = getElementData();
			int newCapacity = (oldCapacity * 3)/2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			setElementData(new PersistentArray(getStore(), Object.class, newCapacity));
			PersistentArray.copy(oldData, 0, getElementData(), 0, getSize());
		}
	}

	public int size() {
		return getSize();
	}

//	public boolean isEmpty() {
//		return getSize() == 0;
//	}

	public boolean contains(Object elem) {
		return indexOf(elem) >= 0;
	}

	public synchronized int indexOf(Object elem) {
		if (elem == null) {
			for (int i = 0; i < getSize(); i++)
				if (getElementData().get(i)==null)
					return i;
		} else {
			for (int i = 0; i < getSize(); i++)
				if (elem.equals(getElementData().get(i)))
					return i;
		}
		return -1;
	}

	public synchronized int lastIndexOf(Object elem) {
		if (elem == null) {
			for (int i = getSize()-1; i >= 0; i--)
				if (getElementData().get(i)==null)
					return i;
		} else {
			for (int i = getSize()-1; i >= 0; i--)
				if (elem.equals(getElementData().get(i)))
					return i;
		}
		return -1;
	}

	public synchronized PersistentObject clone() {
		ArrayList v = (ArrayList)super.clone();
		v.setElementData(new PersistentArray(getStore(), Object.class, getSize()));
		PersistentArray.copy(getElementData(), 0, v.getElementData(), 0, getSize());
		v.setModCount(0);
		return v;
	}

	public synchronized Object[] toArray() {
		Object[] result = new Object[getSize()];
		PersistentArray.copy(getElementData(), 0, result, 0, getSize());
		return result;
	}

	public synchronized Object[] toArray(Object a[]) {
		if (a.length < getSize())
			a = (Object[])java.lang.reflect.Array.newInstance(
				a.getClass().getComponentType(), getSize());

		PersistentArray.copy(getElementData(), 0, a, 0, getSize());

		if (a.length > getSize())
			a[getSize()] = null;

		return a;
	}

	// Positional Access Operations

	public synchronized Object get(int index) {
		RangeCheck(index);

		return getElementData().get(index);
	}

	public synchronized Object set(int index, Object element) {
		RangeCheck(index);

		Object oldValue = getElementData().get(index);
		getElementData().set(index,element);
		return oldValue;
	}

//	public boolean add(Object o) {
//		ensureCapacity(getSize() + 1);  // Increments modCount!!
//		getElementData().set(getSize(),o);
//		setSize(getSize()+1);
//		return true;
//	}

	public synchronized void add(int index, Object element) {
		if (index > getSize() || index < 0)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());
		
		ensureCapacity(getSize()+1);  // Increments modCount!!
		PersistentArray.copy(getElementData(), index, getElementData(), index + 1,
			getSize() - index);
		getElementData().set(index,element);
		setSize(getSize()+1);
	}

	public synchronized Object remove(int index) {
		RangeCheck(index);
		
		setModCount(getModCount()+1);
		Object oldValue = getElementData().get(index);
		
		int numMoved = getSize() - index - 1;
		if (numMoved > 0)
			PersistentArray.copy(getElementData(), index+1, getElementData(), index,
				numMoved);
		setSize(getSize()-1);
		getElementData().set(getSize(),null); // Let gc do its work
		
		return oldValue;
	}

	// Bulk Operations

//	public void clear() {
//		setModCount(getModCount()+1);
//
//		// Let gc do its work
//		for (int i = 0; i < getSize(); i++)
//			getElementData().set(i,null);
//
//		setSize(0);
//	}

//	public boolean addAll(Collection c) {
//		Object[] a = c.toArray();
//		int numNew = a.length;
//		ensureCapacity(getSize() + numNew);  // Increments modCount
//		Arrays.copy(a, 0, getElementData(), getSize(), numNew);
//		setSize(getSize()+numNew);
//		return numNew != 0;
//	}

//	public boolean addAll(int index, Collection c) {
//		if (index > getSize() || index < 0)
//			throw new IndexOutOfBoundsException(
//				"Index: " + index + ", Size: " + getSize());
//
//		Object[] a = c.toArray();
//		int numNew = a.length;
//		ensureCapacity(getSize() + numNew);  // Increments modCount
//
//		int numMoved = getSize() - index;
//		if (numMoved > 0)
//			Arrays.copy(getElementData(), index, getElementData(), index + numNew,
//							 numMoved);
//
//		Arrays.copy(a, 0, getElementData(), index, numNew);
//		setSize(getSize()+numNew);
//		return numNew != 0;
//	}

//	protected void removeRange(int fromIndex, int toIndex) {
//		setModCount(getModCount()+1);
//		int numMoved = getSize() - toIndex;
//		Arrays.copy(getElementData(), toIndex, getElementData(), fromIndex,
//						 numMoved);
//
//		// Let gc do its work
//		int newSize = getSize() - (toIndex-fromIndex);
//		while (getSize() != newSize)
//			setSize(getSize()-1);
//			getElementData().set(getSize(),null);
//	}

	private void RangeCheck(int index) {
		if (index >= size())
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+size());
	}
}
