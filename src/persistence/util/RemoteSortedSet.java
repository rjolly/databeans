/*
 * @(#)SortedSet.java	1.18 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Comparator;

public interface RemoteSortedSet extends RemoteSet {
	Comparator comparator() throws RemoteException;
	RemoteSortedSet subSet(Object fromElement, Object toElement) throws RemoteException;
	RemoteSortedSet headSet(Object toElement) throws RemoteException;
	RemoteSortedSet tailSet(Object fromElement) throws RemoteException;
	Object first() throws RemoteException;
	Object last() throws RemoteException;
}
