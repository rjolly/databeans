/*
 * @(#)SortedSet.java	1.18 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.SortedSet;

public interface RemoteSortedSet extends RemoteSet {
	Comparator comparator() throws RemoteException;
	SortedSet subSet(Object fromElement, Object toElement) throws RemoteException;
	SortedSet headSet(Object toElement) throws RemoteException;
	SortedSet tailSet(Object fromElement) throws RemoteException;
	Object first() throws RemoteException;
	Object last() throws RemoteException;
}
