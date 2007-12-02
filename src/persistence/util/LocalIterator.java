/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Iterator;
import persistence.LocalWrapper;
import persistence.util.RemoteIterator;

class LocalIterator extends LocalWrapper implements Iterator {
	RemoteIterator i;

	LocalIterator(RemoteIterator i) {
		super(i);
		this.i=i;
	}

	public boolean hasNext() {
		try {
			return i.hasNext();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object next() {
		try {
			return i.next();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void remove() {
		try {
			i.remove();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
