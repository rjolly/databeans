/*
 * @(#)TreeMap.java		1.56 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

public class SubMap extends AbstractMap
			implements SortedMap {
	public TreeMap getMap() {
		return (TreeMap)get("map");
	}

	public void setMap(TreeMap map) {
		set("map",map);
	}

	public boolean getFromStart() {
		return ((Boolean)get("fromStart")).booleanValue();
	}

	public void setFromStart(boolean b) {
		set("fromStart",new Boolean(b));
	}

	public boolean getToEnd() {
		return ((Boolean)get("toEnd")).booleanValue();
	}

	public void setToEnd(boolean b) {
		set("toEnd",new Boolean(b));
	}

	public Object getFromKey() {
		return get("fromKey");
	}

	public void setFromKey(Object obj) {
		set("fromKey",obj);
	}

	public Object getToKey() {
		return get("toKey");
	}

	public void setToKey(Object obj) {
		set("toKey",obj);
	}

	public void init(TreeMap map, Object fromKey, Object toKey) {
		if (map.compare(fromKey, toKey) > 0)
			throw new IllegalArgumentException("fromKey > toKey");
		setMap(map);
		setFromKey(fromKey);
		setToKey(toKey);
	}

	public void init(TreeMap map, Object key, boolean headMap) {
		map.compare(key, key); // Type-check key

		setMap(map);
		if (headMap) {
			setFromStart(true);
			setToKey(key);
		} else {
			setToEnd(true);
			setFromKey(key);
		}
	}

	public void init(TreeMap map, boolean fromStart, Object fromKey, boolean toEnd, Object toKey){
		setMap(map);
		setFromStart(fromStart);
		setFromKey(fromKey);
		setToEnd(toEnd);
		setToKey(toKey);
	}

	public boolean isEmpty() {
		return entrySet.isEmpty();
	}

	public boolean containsKey(Object key) {
		return inRange(key) && getMap().containsKey(key);
	}

	public Object get(Object key) {
		if (!inRange(key))
			return null;
		return getMap().get(key);
	}

	public Object put(Object key, Object value) {
		if (!inRange(key))
			throw new IllegalArgumentException("key out of range");
		return getMap().put(key, value);
	}

	public Comparator comparator() {
		return getMap().comparator();
	}

	public Object firstKey() {
		Object first = getMap().key(getFromStart() ? getMap().firstEntry():getMap().getCeilEntry(getFromKey()));
		if (!getToEnd() && getMap().compare(first, getToKey()) >= 0)
			throw(new NoSuchElementException());
		return first;
	}

	public Object lastKey() {
		Object last = getMap().key(getToEnd() ? getMap().lastEntry() : getMap().getPrecedingEntry(getToKey()));
		if (!getFromStart() && getMap().compare(last, getFromKey()) < 0)
			throw(new NoSuchElementException());
		return last;
	}

	private transient Set entrySet = null;

	public Set entrySet() {
		return entrySet==null?entrySet=(Set)create(EntrySetView.class,new Class[] {SubMap.class},new Object[] {this}):entrySet;
	}

	public SortedMap subMap(Object fromKey, Object toKey) {
		if (!inRange2(fromKey))
			throw new IllegalArgumentException("fromKey out of range");
		if (!inRange2(toKey))
			throw new IllegalArgumentException("toKey out of range");
		return (SortedMap)create(SubMap.class,new Class[] {TreeMap.class,Object.class,Object.class},new Object[] {getMap(),fromKey,toKey});
	}

	public SortedMap headMap(Object toKey) {
		if (!inRange2(toKey))
			throw new IllegalArgumentException("toKey out of range");
		return (SortedMap)create(SubMap.class,new Class[] {TreeMap.class,boolean.class,Object.class,boolean.class,Object.class},new Object[] {getMap(),new Boolean(getFromStart()),getFromKey(),new Boolean(false),toKey});
	}

	public SortedMap tailMap(Object fromKey) {
		if (!inRange2(fromKey))
			throw new IllegalArgumentException("fromKey out of range");
		return (SortedMap)create(SubMap.class,new Class[] {TreeMap.class,boolean.class,Object.class,boolean.class,Object.class},new Object[] {getMap(),new Boolean(false),fromKey,new Boolean(getToEnd()),getToKey()});
	}

	boolean inRange(Object key) {
		return (getFromStart() || getMap().compare(key, getFromKey()) >= 0) &&
			   (getToEnd() || getMap().compare(key, getToKey()) < 0);
	}

	// This form allows the high endpoint (as well as all legit keys)
	private boolean inRange2(Object key) {
		return (getFromStart() || getMap().compare(key, getFromKey()) >= 0) &&
			   (getToEnd() || getMap().compare(key, getToKey()) <= 0);
	}
}
