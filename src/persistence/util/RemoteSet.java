/*
 * @(#)Set.java	1.29 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;

public interface RemoteSet extends RemoteCollection {
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
	boolean retainAll(Collection c) throws RemoteException;
	boolean removeAll(Collection c) throws RemoteException;
	void clear() throws RemoteException;
//	boolean equals(Object o);
//	int hashCode();
}
