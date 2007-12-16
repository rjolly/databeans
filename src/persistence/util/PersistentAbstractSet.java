/*
 * @(#)AbstractSet.java		1.19 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import persistence.PersistentObject;

public abstract class PersistentAbstractSet extends PersistentAbstractCollection implements Set {
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentAbstractCollection.Accessor {
		public Accessor() throws RemoteException {}

		// Comparison and hashing

		public boolean remoteEquals(PersistentObject o) {
			if (o == PersistentAbstractSet.this)
				return true;

			if (!(o instanceof Set))
				return false;
			Collection c = (Collection) o;
			if (c.size() != size())
				return false;
			try {
				return containsAll(c);
			} catch(ClassCastException unused)   {
				return false;
			} catch(NullPointerException unused) {
				return false;
			}
		}

		public int remoteHashCode() {
			int h = 0;
			Iterator i = iterator();
			while (i.hasNext()) {
				Object obj = i.next();
				if (obj != null)
					h += obj.hashCode();
			}
			return h;
		}
	}

	public boolean removeAll(Collection c) {
		boolean modified = false;

		if (size() > c.size()) {
			for (Iterator i = c.iterator(); i.hasNext(); )
				modified |= remove(i.next());
		} else {
			for (Iterator i = iterator(); i.hasNext(); ) {
				if(c.contains(i.next())) {
					i.remove();
					modified = true;
				}
			}
		}
		return modified;
	}
}
