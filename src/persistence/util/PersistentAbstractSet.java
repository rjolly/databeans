/*
 * @(#)AbstractSet.java	1.19 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import persistence.Accessor;
import persistence.Connection;

public abstract class PersistentAbstractSet extends PersistentAbstractCollection implements RemoteSet {
	public PersistentAbstractSet() throws RemoteException {}

	public PersistentAbstractSet(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Set))
			return false;
		Collection c = (Collection) o;
		if (c.size() != size())
			return false;
		return containsAll(c);
	}

	public int hashCode() {
		int h = 0;
		Iterator i = ((Set)local()).iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			if (obj != null)
				h += obj.hashCode();
		}
		return h;
	}

	public boolean removeAll(Collection c) {
	synchronized(mutex()) {
		boolean modified = false;

		if (size() > c.size()) {
			for (Iterator i = c.iterator(); i.hasNext(); )
				modified |= remove(i.next());
		} else {
			for (Iterator i = ((Set)local()).iterator(); i.hasNext(); ) {
				if(c.contains(i.next())) {
					i.remove();
					modified = true;
				}
			}
		}
		return modified;
	}
	}

	public Object local() {
		return new LocalSet(this);
	}
}
