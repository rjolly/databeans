/*
 * @(#)SortedMap.java	1.15 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Comparator;

public interface RemoteSortedMap extends RemoteMap {
	Comparator comparator() throws RemoteException;
	RemoteSortedMap subMap(Object fromKey, Object toKey) throws RemoteException;
	RemoteSortedMap headMap(Object toKey) throws RemoteException;
	RemoteSortedMap tailMap(Object fromKey) throws RemoteException;
	Object firstKey() throws RemoteException;
	Object lastKey() throws RemoteException;
}
