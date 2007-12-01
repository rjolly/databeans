/*
 * @(#)Map.java	1.39 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import persistence.Persistent;

public interface RemoteMap extends Persistent {
	int size() throws RemoteException;
	boolean isEmpty() throws RemoteException;
	boolean containsKey(Object key) throws RemoteException;
	boolean containsValue(Object value) throws RemoteException;
	Object get(Object key) throws RemoteException;
	Object put(Object key, Object value) throws RemoteException;
	Object remove(Object key) throws RemoteException;
	void putAll(RemoteMap t) throws RemoteException;
	void clear() throws RemoteException;
	public RemoteSet keySet() throws RemoteException;
	public RemoteCollection values() throws RemoteException;
	public RemoteSet entrySet() throws RemoteException;

	public interface Entry extends Persistent {
		Object getKey() throws RemoteException;
		Object getValue() throws RemoteException;
		Object setValue(Object value) throws RemoteException;
//		boolean equals(Object o);
//		int hashCode();
	}

//	boolean equals(Object o);
//	int hashCode();
}
