/*
 * @(#)AbstractSet.java		1.19 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import persistence.PersistentObject;
import persistence.Store;

public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {
	public AbstractSet() {
	}

	public AbstractSet(final Store store) {
		super(store);
	}

	public boolean removeAll(Collection<?> c) {
		boolean modified = false;

		if (size() > c.size()) {
			for (Iterator<?> i = c.iterator(); i.hasNext(); )
				modified |= remove(i.next());
		} else {
			for (Iterator<?> i = iterator(); i.hasNext(); ) {
				if(c.contains(i.next())) {
					i.remove();
					modified = true;
				}
			}
		}
		return modified;
	}

	// Comparison and hashing

	public synchronized boolean equals(PersistentObject o) {
		if (o == AbstractSet.this)
			return true;

		if (!(o instanceof Set))
			return false;
		Collection<?> c = (Collection<?>) o;
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

	public synchronized int hashCode() {
		int h = 0;
		Iterator<E> i = iterator();
		while (i.hasNext()) {
			E obj = i.next();
			if (obj != null)
				h += obj.hashCode();
		}
		return h;
	}
}
