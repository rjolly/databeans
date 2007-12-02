/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Map;
import persistence.LocalWrapper;
import persistence.util.RemoteMap;

class LocalEntry extends LocalWrapper implements Map.Entry {
	RemoteMap.Entry e;

	LocalEntry(RemoteMap.Entry e) {
		super(e);
		this.e = e;
	}

	public Object getKey() {
		try {
			return e.getKey();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object getValue() {
		try {
			return e.getValue();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object setValue(Object value) {
		try {
			return e.setValue(value);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
