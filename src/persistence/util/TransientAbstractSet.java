/*
 * @(#)AbstractSet.java	1.19 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.*;
import java.rmi.*;

public abstract class TransientAbstractSet extends TransientAbstractCollection implements RemoteSet {
	protected TransientAbstractSet() throws RemoteException {
	}

	protected TransientAbstractSet(Object mutex) throws RemoteException {
		super(mutex);
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Set))
			return false;
		RemoteCollection c = (RemoteCollection) o;
		if (PersistentCollections.localCollection(c).size() != size())
			return false;
		return containsAll(c);
	}

	public int hashCode() {
		int h = 0;
		Iterator i = PersistentCollections.localSet(this).iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			if (obj != null)
				h += obj.hashCode();
		}
		return h;
	}

	public boolean removeAll(RemoteCollection c) {
	synchronized(mutex) {
		boolean modified = false;

		if (size() > PersistentCollections.localCollection(c).size()) {
			for (Iterator i = PersistentCollections.localCollection(c).iterator(); i.hasNext(); )
				modified |= remove(i.next());
		} else {
			for (Iterator i = PersistentCollections.localSet(this).iterator(); i.hasNext(); ) {
				if(PersistentCollections.localCollection(c).contains(i.next())) {
					i.remove();
					modified = true;
				}
			}
		}
		return modified;
	}
	}
}
