/*
 * @(#)List.java	1.39 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.*;

public interface RemoteList extends RemoteCollection {
	int size() throws RemoteException;
	boolean isEmpty() throws RemoteException;
	boolean contains(Object o) throws RemoteException;
	RemoteIterator iterator() throws RemoteException;
	Object[] toArray() throws RemoteException;
	Object[] toArray(Object a[]) throws RemoteException;
	boolean add(Object o) throws RemoteException;
	boolean remove(Object o) throws RemoteException;
	boolean containsAll(RemoteCollection c) throws RemoteException;
	boolean addAll(RemoteCollection c) throws RemoteException;
	boolean addAll(int index, RemoteCollection c) throws RemoteException;
	boolean removeAll(RemoteCollection c) throws RemoteException;
	boolean retainAll(RemoteCollection c) throws RemoteException;
	void clear() throws RemoteException;
//	boolean equals(Object o);
//	int hashCode();
	Object get(int index) throws RemoteException;
	Object set(int index, Object element) throws RemoteException;
	void add(int index, Object element) throws RemoteException;
	Object remove(int index) throws RemoteException;
	int indexOf(Object o) throws RemoteException;
	int lastIndexOf(Object o) throws RemoteException;
	RemoteListIterator listIterator() throws RemoteException;
	RemoteListIterator listIterator(int index) throws RemoteException;
	RemoteList subList(int fromIndex, int toIndex) throws RemoteException;
}
