/*
 * @(#)AbstractList.java	1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.List;
import java.util.RandomAccess;

class RandomAccessSubList extends SubList implements RandomAccess {
	RandomAccessSubList(List list, int fromIndex, int toIndex) {
		super(list, fromIndex, toIndex);
	}

	public List subList(int fromIndex, int toIndex) {
		return new RandomAccessSubList(this, fromIndex, toIndex);
	}
}
