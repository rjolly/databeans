/*
 * @(#)TreeMap.java		1.56 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Iterator;
import java.util.Map;

public class EntrySetView extends AbstractSet {
	public SubMap getMap() {
		return (SubMap)get("map");
	}

	public void setMap(SubMap map) {
		set("map",map);
	}

	public int getSize() {
		return ((Integer)get("size")).intValue();
	}

	public void setSize(int n) {
		set("size",new Integer(n));
	}

	public int getSizeModCount() {
		return ((Integer)get("sizeModCount")).intValue();
	}

	public void setSizeModCount(int n) {
		set("sizeModCount",new Integer(n));
	}

	public void init(SubMap map) {
		setMap(map);
		setSize(-1);
	}

	public int size() {
		if (getSize() == -1 || getSizeModCount() != getMap().getMap().modCount()) {
			setSize(0);  setSizeModCount(getMap().getMap().modCount());
			Iterator i = iterator();
			while (i.hasNext()) {
				setSize(getSize()+1);
				i.next();
			}
		}
		return getSize();
	}

	public boolean isEmpty() {
		return !iterator().hasNext();
	}

	public boolean contains(Object o) {
		if (!(o instanceof Map.Entry))
			return false;
		Map.Entry entry = (Map.Entry)o;
		Object key = entry.getKey();
		if (!getMap().inRange(key))
			return false;
		TreeMap.Entry node = getMap().getMap().getEntry(key);
		return node != null &&
			   getMap().getMap().valEquals(node.getValue(), entry.getValue());
	}

	public boolean remove(Object o) {
		if (!(o instanceof Map.Entry))
			return false;
		Map.Entry entry = (Map.Entry)o;
		Object key = entry.getKey();
		if (!getMap().inRange(key))
			return false;
		TreeMap.Entry node = getMap().getMap().getEntry(key);
		if (node!=null && getMap().getMap().valEquals(node.getValue(),entry.getValue())){
			getMap().getMap().deleteEntry(node);
			return true;
		}
		return false;
	}

	public Iterator iterator() {
		return getMap().getMap().new SubMapEntryIterator(
			(getMap().getFromStart() ? getMap().getMap().firstEntry() : getMap().getMap().getCeilEntry(getMap().getFromKey())),
			(getMap().getToEnd() ? null : getMap().getMap().getCeilEntry(getMap().getToKey())));
	}

	protected boolean unchecked(String property) {
		return super.unchecked(property) || property.equals("map") || property.equals("size") || property.equals("sizeModCount");
	}
}
