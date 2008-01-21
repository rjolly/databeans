/*
 * @(#)LinkedHashSet.java	1.8 03/01/20
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Set;

public class PersistentLinkedHashSet extends PersistentHashSet
	implements Set, Cloneable {
	public void init(int initialCapacity, float loadFactor) {
		super.init(initialCapacity, loadFactor, true);
	}

	public void init(int initialCapacity) {
		super.init(initialCapacity, .75f, true);
	}

	public  void init() {
		super.init(16, .75f, true);
	}

	public  void init(Collection c) {
		super.init(Math.max(2*c.size(), 11), .75f, true);
		addAll(c);
	}
}
