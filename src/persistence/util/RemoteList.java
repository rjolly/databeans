/*
 * @(#)List.java	1.39 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public interface RemoteList extends RemoteCollection {
	int size() throws RemoteException;
	boolean isEmpty() throws RemoteException;
	boolean contains(Object o) throws RemoteException;
	Iterator iterator() throws RemoteException;
	Object[] toArray() throws RemoteException;
	Object[] toArray(Object a[]) throws RemoteException;
	boolean add(Object o) throws RemoteException;
	boolean remove(Object o) throws RemoteException;
	boolean containsAll(Collection c) throws RemoteException;
	boolean addAll(Collection c) throws RemoteException;
	boolean addAll(int index, Collection c) throws RemoteException;
	boolean removeAll(Collection c) throws RemoteException;
	boolean retainAll(Collection c) throws RemoteException;
	void clear() throws RemoteException;
//	boolean equals(Object o);
//	int hashCode();
	Object get(int index) throws RemoteException;
	Object set(int index, Object element) throws RemoteException;
	void add(int index, Object element) throws RemoteException;
	Object remove(int index) throws RemoteException;
	int indexOf(Object o) throws RemoteException;
	int lastIndexOf(Object o) throws RemoteException;
	ListIterator listIterator() throws RemoteException;
	ListIterator listIterator(int index) throws RemoteException;
	List subList(int fromIndex, int toIndex) throws RemoteException;
}
