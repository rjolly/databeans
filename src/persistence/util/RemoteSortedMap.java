/*
 * @(#)SortedMap.java	1.15 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.SortedMap;

public interface RemoteSortedMap extends RemoteMap {
	Comparator comparator() throws RemoteException;
	SortedMap subMap(Object fromKey, Object toKey) throws RemoteException;
	SortedMap headMap(Object toKey) throws RemoteException;
	SortedMap tailMap(Object fromKey) throws RemoteException;
	Object firstKey() throws RemoteException;
	Object lastKey() throws RemoteException;
}
