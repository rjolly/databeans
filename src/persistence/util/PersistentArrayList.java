/*
 * @(#)ArrayList.java	1.43 04/12/09
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.*;
import java.rmi.*;
import persistence.*;

public class PersistentArrayList extends PersistentAbstractList implements RemoteList {
	public RemoteArray getElementData() {
		return (RemoteArray)get("elementData");
	}

	public void setElementData(RemoteArray array) {
		set("elementData",array);
	}

	public int getSize() {
		return ((Integer)get("size")).intValue();
	}

	public void setSize(int n) {
		set("size",new Integer(n));
	}

	public PersistentArrayList() throws RemoteException {}

	public PersistentArrayList(Accessor accessor, Connection connection, Integer initialCapacity) throws RemoteException {
		super(accessor,connection);
		if (initialCapacity.intValue() < 0) throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity.intValue());
		setElementData(create(Object.class,initialCapacity.intValue()));
	}

	public PersistentArrayList(Accessor accessor, Connection connection) throws RemoteException {
		this(accessor,connection,new Integer(10));
	}

	public void trimToSize() {
	synchronized(mutex) {
		setModCount(getModCount()+1);
		int oldCapacity = PersistentArrays.localArray(getElementData()).length();
		if (getSize() < oldCapacity) {
			Array oldData = PersistentArrays.localArray(getElementData());
			setElementData(create(Object.class,getSize()));
			PersistentArrays.copy(oldData, 0, PersistentArrays.localArray(getElementData()), 0, getSize());
		}
	}
	}

	public void ensureCapacity(int minCapacity) {
	synchronized(mutex) {
		setModCount(getModCount()+1);
		int oldCapacity = PersistentArrays.localArray(getElementData()).length();
		if (minCapacity > oldCapacity) {
			Array oldData = PersistentArrays.localArray(getElementData());
			int newCapacity = (oldCapacity * 3)/2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			setElementData(create(Object.class,newCapacity));
			PersistentArrays.copy(oldData, 0, PersistentArrays.localArray(getElementData()), 0, getSize());
		}
	}
	}

	public int size() {
	synchronized(mutex) {
		return getSize();
	}
	}

	public boolean isEmpty() {
	synchronized(mutex) {
		return getSize() == 0;
	}
	}

	public boolean contains(Object elem) {
	synchronized(mutex) {
		return indexOf(elem) >= 0;
	}
	}

	public int indexOf(Object elem) {
	synchronized(mutex) {
		if (elem == null) {
			for (int i = 0; i < getSize(); i++)
				if (PersistentArrays.localArray(getElementData()).get(i)==null)
					return i;
		} else {
			for (int i = 0; i < getSize(); i++)
				if (elem.equals(PersistentArrays.localArray(getElementData()).get(i)))
					return i;
		}
		return -1;
	}
	}

	public int lastIndexOf(Object elem) {
	synchronized(mutex) {
		if (elem == null) {
			for (int i = getSize()-1; i >= 0; i--)
				if (PersistentArrays.localArray(getElementData()).get(i)==null)
					return i;
		} else {
			for (int i = getSize()-1; i >= 0; i--)
				if (elem.equals(PersistentArrays.localArray(getElementData()).get(i)))
					return i;
		}
		return -1;
	}
	}

	public Object[] toArray() {
	synchronized(mutex) {
		Object[] result = new Object[getSize()];
		PersistentArrays.copy(PersistentArrays.localArray(getElementData()), 0, result, 0, getSize());
		return result;
	}
	}

	public Object[] toArray(Object a[]) {
	synchronized(mutex) {
		if (a.length < getSize())
			a = (Object[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), getSize());

		PersistentArrays.copy(PersistentArrays.localArray(getElementData()), 0, a, 0, getSize());

		if (a.length > getSize())
			a[getSize()] = null;

		return a;
	}
	}

	public Object get(int index) {
	synchronized(mutex) {
		RangeCheck(index);

		return PersistentArrays.localArray(getElementData()).get(index);
	}
	}

	public Object set(int index, Object element) {
	synchronized(mutex) {
		RangeCheck(index);

		Object oldValue = PersistentArrays.localArray(getElementData()).get(index);
		PersistentArrays.localArray(getElementData()).set(index,element);
		return oldValue;
	}
	}

	public boolean add(Object o) {
	synchronized(mutex) {
		ensureCapacity(getSize() + 1);  // Increments modCount!!
		PersistentArrays.localArray(getElementData()).set(getSize(),o);
		setSize(getSize()+1);
		return true;
	}
	}

	public void add(int index, Object element) {
	synchronized(mutex) {
		if (index > getSize() || index < 0)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());

		ensureCapacity(getSize()+1);  // Increments modCount!!
		PersistentArrays.copy(PersistentArrays.localArray(getElementData()), index, PersistentArrays.localArray(getElementData()), index + 1,
						 getSize() - index);
		PersistentArrays.localArray(getElementData()).set(index,element);
		setSize(getSize()+1);
	}
	}

	public Object remove(int index) {
	synchronized(mutex) {
		RangeCheck(index);

		setModCount(getModCount()+1);
		Object oldValue = PersistentArrays.localArray(getElementData()).get(index);

		int numMoved = getSize() - index - 1;
		if (numMoved > 0)
			PersistentArrays.copy(PersistentArrays.localArray(getElementData()), index+1, PersistentArrays.localArray(getElementData()), index,
							 numMoved);
		setSize(getSize()-1);
		PersistentArrays.localArray(getElementData()).set(getSize(),null); // Let gc do its work

		return oldValue;
	}
	}

	public void clear() {
	synchronized(mutex) {
		setModCount(getModCount()+1);

		for (int i = 0; i < getSize(); i++)
			PersistentArrays.localArray(getElementData()).set(i,null);

		setSize(0);
	}
	}

	public boolean addAll(RemoteCollection c) {
	synchronized(mutex) {
		setModCount(getModCount()+1);
		int numNew = PersistentCollections.localCollection(c).size();
		ensureCapacity(getSize() + numNew);

		Iterator e = PersistentCollections.localCollection(c).iterator();
		for (int i=0; i<numNew; i++) {
			PersistentArrays.localArray(getElementData()).set(getSize(),e.next());
			setSize(getSize()+1);
		}
		return numNew != 0;
	}
	}

	public boolean addAll(int index, RemoteCollection c) {
	synchronized(mutex) {
		if (index > getSize() || index < 0)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());

		int numNew = PersistentCollections.localCollection(c).size();
		ensureCapacity(getSize() + numNew);  // Increments modCount!!

		int numMoved = getSize() - index;
		if (numMoved > 0)
			PersistentArrays.copy(PersistentArrays.localArray(getElementData()), index, PersistentArrays.localArray(getElementData()), index + numNew,
							 numMoved);

		Iterator e = PersistentCollections.localCollection(c).iterator();
		for (int i=0; i<numNew; i++)
			PersistentArrays.localArray(getElementData()).set(index++,e.next());

		setSize(getSize()+numNew);
		return numNew != 0;
	}
	}

	protected void removeRange(int fromIndex, int toIndex) {
		setModCount(getModCount()+1);
		int numMoved = getSize() - toIndex;
		PersistentArrays.copy(PersistentArrays.localArray(getElementData()), toIndex, PersistentArrays.localArray(getElementData()), fromIndex,
						 numMoved);

		// Let gc do its work
		int newSize = getSize() - (toIndex-fromIndex);
		while (getSize() != newSize) {
			setSize(getSize()-1);
			PersistentArrays.localArray(getElementData()).set(getSize(),null);
		}
	}

	private void RangeCheck(int index) {
		if (index >= getSize() || index < 0)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());
	}
}
