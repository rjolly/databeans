/*
 * @(#)ListIterator.java	1.21 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;

public interface RemoteListIterator extends RemoteIterator {
	boolean hasNext() throws RemoteException;
	Object next() throws RemoteException;
	boolean hasPrevious() throws RemoteException;
	Object previous() throws RemoteException;
	int nextIndex() throws RemoteException;
	int previousIndex() throws RemoteException;
	void remove() throws RemoteException;
	void set(Object o) throws RemoteException;
	void add(Object o) throws RemoteException;
}
