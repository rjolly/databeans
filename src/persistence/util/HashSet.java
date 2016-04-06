/*
 * @(#)HashSet.java		1.28 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import persistence.PersistentClass;
import persistence.PersistentObject;
import persistence.Store;

public class HashSet extends AbstractSet implements Set, Cloneable {
	public HashSet() {
	}

	public HashSet(final Store store) {
		this(store, new HashMap(store));
	}

	public HashSet(final Store store, Collection c) {
		this(store, new HashMap(store, Math.max((int) (c.size()/.75f) + 1, 16)));
		addAll(c);
	}

	public HashSet(final Store store, int initialCapacity, float loadFactor) {
		this(store, new HashMap(store, initialCapacity, loadFactor));
	}

	public HashSet(final Store store, int initialCapacity) {
		this(store, new HashMap(store, initialCapacity));
	}

	public HashSet(final Store store, int initialCapacity, float loadFactor, boolean dummy) {
		this(store, new LinkedHashMap(store, initialCapacity, loadFactor));
	}

	HashSet(final Store store, HashMap map) {
		super(store);
		setMap(map);
	}

	protected PersistentClass createClass() {
		return new HashSetClass(getStore());
	}

	HashMap map() {
		return getMap();
	}

	public HashMap getMap() {
		return (HashMap)get("map");
	}

	public void setMap(HashMap map) {
		set("map",map);
	}

	public Iterator iterator() {
		return map().keySet().iterator();
	}

	public int size() {
		return map().size();
	}

	public boolean isEmpty() {
		return map().isEmpty();
	}

	public boolean contains(Object o) {
		return map().containsKey(o);
	}

	public boolean add(Object o) {
		return map().put(o, persistentClass().PRESENT())==null;
	}

	public boolean remove(Object o) {
		return map().remove(o)==persistentClass().PRESENT();
	}

	public void clear() {
		map().clear();
	}

	public final HashSetClass persistentClass() {
		return (HashSetClass)super.persistentClass();
	}

	public synchronized PersistentObject clone() {
		HashSet newSet = (HashSet)super.clone();
		newSet.setMap((HashMap)getMap().clone());
		return newSet;
	}
}
