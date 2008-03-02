/*
 * @(#)AbstractList.java		1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.List;
import java.util.RandomAccess;

public class RandomAccessSubList extends SubList implements RandomAccess {
	public void init(AbstractList list, int fromIndex, int toIndex) {
		super.init(list, fromIndex, toIndex);
	}

	public List subList(int fromIndex, int toIndex) {
		return (List)create(RandomAccessSubList.class,new Class[] {AbstractList.class,int.class,int.class},new Object[] {getL(),new Integer(getOffset()+fromIndex),new Integer(getOffset()+toIndex)});
	}
}
