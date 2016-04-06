/*
 * @(#)LinkedHashSet.java	1.8 03/01/20
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Set;
import persistence.Store;

public class LinkedHashSet extends HashSet implements Set, Cloneable {
	public LinkedHashSet() {
	}

	public LinkedHashSet(final Store store, int initialCapacity, float loadFactor) {
		super(store, initialCapacity, loadFactor, true);
	}

	public LinkedHashSet(final Store store, int initialCapacity) {
		super(store, initialCapacity, .75f, true);
	}

	public LinkedHashSet(final Store store) {
		super(store, 16, .75f, true);
	}

	public LinkedHashSet(final Store store, Collection c) {
		super(store, Math.max(2*c.size(), 11), .75f, true);
		addAll(c);
	}
}
