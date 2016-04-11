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

public class HashSet<E> extends AbstractSet<E> implements Set<E> {
	public HashSet() {
	}

	public HashSet(final Store store) {
		this(store, new HashMap<E,Object>(store));
	}

	public HashSet(final Store store, Collection<? extends E> c) {
		this(store, new HashMap<E,Object>(store, Math.max((int) (c.size()/.75f) + 1, 16)));
		addAll(c);
	}

	public HashSet(final Store store, int initialCapacity, float loadFactor) {
		this(store, new HashMap<E,Object>(store, initialCapacity, loadFactor));
	}

	public HashSet(final Store store, int initialCapacity) {
		this(store, new HashMap<E,Object>(store, initialCapacity));
	}

	public HashSet(final Store store, int initialCapacity, float loadFactor, boolean dummy) {
		this(store, new LinkedHashMap<E,Object>(store, initialCapacity, loadFactor));
	}

	HashSet(final Store store, HashMap<E,Object> map) {
		super(store);
		setMap(map);
	}

	protected PersistentClass createClass() {
		return getClass() == HashSet.class?new HashSetClass(this):super.createClass();
	}

	HashMap<E,Object> map() {
		return getMap();
	}

	public HashMap<E,Object> getMap() {
		return get("map");
	}

	public void setMap(HashMap<E,Object> map) {
		set("map",map);
	}

	public Iterator<E> iterator() {
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

	Object PRESENT() {
		return ((HashSetClass)getStore().get(HashSet.class)).PRESENT();
	}

	public boolean add(E e) {
		return map().put(e, PRESENT())==null;
	}

	public boolean remove(Object o) {
		return map().remove(o)==PRESENT();
	}

	public void clear() {
		map().clear();
	}

	@SuppressWarnings("unchecked")
	public synchronized PersistentObject clone() {
		HashSet<E> newSet = (HashSet<E>)super.clone();
		newSet.setMap((HashMap<E, Object>)getMap().clone());
		return newSet;
	}
}
