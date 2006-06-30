/*
 * @(#)Iterator.java	1.18 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.*;

public interface RemoteIterator extends Remote {
	boolean hasNext() throws RemoteException;
	Object next() throws RemoteException;
	void remove() throws RemoteException;
}
