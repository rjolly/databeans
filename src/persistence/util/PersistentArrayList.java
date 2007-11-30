/*
 * @(#)ArrayList.java	1.43 04/12/09
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import persistence.Accessor;
import persistence.Array;
import persistence.Connection;
import persistence.PersistentArrays;
import persistence.RemoteArray;

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
	synchronized(mutex()) {
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
	synchronized(mutex()) {
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
	synchronized(mutex()) {
		return getSize();
	}
	}

	public boolean isEmpty() {
	synchronized(mutex()) {
		return getSize() == 0;
	}
	}

	public boolean contains(Object elem) {
	synchronized(mutex()) {
		return indexOf(elem) >= 0;
	}
	}

	int indexOf0(Object elem) {
	synchronized(mutex()) {
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

	int lastIndexOf0(Object elem) {
	synchronized(mutex()) {
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
	synchronized(mutex()) {
		Object[] result = new Object[getSize()];
		PersistentArrays.copy(PersistentArrays.localArray(getElementData()), 0, result, 0, getSize());
		return result;
	}
	}

	public Object[] toArray(Object a[]) {
	synchronized(mutex()) {
		if (a.length < getSize())
			a = (Object[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), getSize());

		PersistentArrays.copy(PersistentArrays.localArray(getElementData()), 0, a, 0, getSize());

		if (a.length > getSize())
			a[getSize()] = null;

		return a;
	}
	}

	Object get0(int index) {
	synchronized(mutex()) {
		RangeCheck(index);

		return PersistentArrays.localArray(getElementData()).get(index);
	}
	}

	Object set0(int index, Object element) {
	synchronized(mutex()) {
		RangeCheck(index);

		Object oldValue = PersistentArrays.localArray(getElementData()).get(index);
		PersistentArrays.localArray(getElementData()).set(index,element);
		return oldValue;
	}
	}

	int add0(int index, Object element) {
	synchronized(mutex()) {
		if (index > getSize() || index < 0)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());

		ensureCapacity(getSize()+1);  // Increments modCount!!
		PersistentArrays.copy(PersistentArrays.localArray(getElementData()), index, PersistentArrays.localArray(getElementData()), index + 1,
						 getSize() - index);
		PersistentArrays.localArray(getElementData()).set(index,element);
		setSize(getSize()+1);
		return index;
	}
	}

	Object remove0(int index) {
	synchronized(mutex()) {
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

	private void RangeCheck(int index) {
		if (index >= getSize() || index < 0)
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+getSize());
	}
}
