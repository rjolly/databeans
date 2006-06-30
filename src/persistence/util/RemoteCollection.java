/*
 * @(#)Collection.java	1.39 03/01/17
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.*;

public interface RemoteCollection extends Remote {
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
	boolean removeAll(RemoteCollection c) throws RemoteException;
	boolean retainAll(RemoteCollection c) throws RemoteException;
	void clear() throws RemoteException;
//	boolean equals(Object o);
//	int hashCode();
	String remoteToString() throws RemoteException;
}
