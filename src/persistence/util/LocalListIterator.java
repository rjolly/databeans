/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.ListIterator;
import persistence.util.RemoteListIterator;

class LocalListIterator extends LocalIterator implements ListIterator {
	RemoteListIterator li;

	LocalListIterator(RemoteListIterator i) {
		super(i);
		li=i;
	}

	public boolean hasPrevious() {
		try {
			return li.hasPrevious();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object previous() {
		try {
			return li.previous();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public int nextIndex() {
		try {
			return li.nextIndex();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public int previousIndex() {
		try {
			return li.previousIndex();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void set(Object o) {
		try {
			li.set(o);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void add(Object o) {
		try {
			li.add(o);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
