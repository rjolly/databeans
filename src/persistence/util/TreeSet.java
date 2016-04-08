/*
 * @(#)TreeSet.java		1.26 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import persistence.PersistentClass;
import persistence.PersistentObject;
import persistence.Store;

public class TreeSet extends AbstractSet implements SortedSet, Cloneable {
	public TreeSet() {
	}

	public TreeSet(final Store store, SortedMap map) {
		super(store);
		setM(map);
	}

	public TreeSet(final Store store) {
		this(store, new TreeMap(store));
	}

	public TreeSet(final Store store, Comparator c) {
		this(store, new TreeMap(store, c));
	}

	public TreeSet(final Store store, Collection c) {
		this(store);
		addAll(c);		
	}

	public TreeSet(final Store store, SortedSet s) {
		this(store, s.comparator());
		addAll(s);
	}

	protected PersistentClass createClass() {
		return getClass() == TreeSet.class?new TreeSetClass(this):super.createClass();
	}

	SortedMap m() {
		return getM();
	}

	Set keySet() {
		return m().keySet();
	}

	public SortedMap getM() {
		return (SortedMap)get("m");
	}

	public void setM(SortedMap map) {
		set("m",map);
	}

	public Iterator iterator() {
		return keySet().iterator();
	}

	public int size() {
		return m().size();
	}

	public boolean isEmpty() {
		return m().isEmpty();
	}

	public boolean contains(Object o) {
		return m().containsKey(o);
	}

	Object PRESENT() {
		return ((TreeSetClass)getStore().get(TreeSet.class)).PRESENT();
	}

	public boolean add(Object o) {
		return m().put(o, PRESENT())==null;
	}

	public boolean remove(Object o) {
		return m().remove(o)==PRESENT();
	}

	public void clear() {
		m().clear();
	}

//	public boolean addAll(Collection c) {
//		// Use linear-time version if applicable
//		if (m().size()==0 && c.size() > 0 && c instanceof SortedSet && 
//			m() instanceof TreeMap) {
//			SortedSet set = (SortedSet)c;
//			TreeMap map = (TreeMap)m();
//			Comparator cc = set.comparator();
//			Comparator mc = map.comparator();
//			if (cc==mc || (cc != null && cc.equals(mc))) {
//				map.addAllForTreeSet(set, persistentClass().PRESENT());
//				return true;
//			}
//		}
//		return super.addAll(c);
//	}

	public SortedSet subSet(Object fromElement, Object toElement) {
		return new TreeSetView(this, m().subMap(fromElement, toElement));
	}

	public SortedSet headSet(Object toElement) {
		return new TreeSetView(this, m().headMap(toElement));
	}

	public SortedSet tailSet(Object fromElement) {
		return new TreeSetView(this, m().tailMap(fromElement));
	}

	public Comparator comparator() {
		return m().comparator();
	}

	public Object first() {
		return m().firstKey();
	}

	public Object last() {
		return m().lastKey();
	}

	public synchronized PersistentObject clone() {
		TreeSet clone = (TreeSet)super.clone();
		clone.setM(new TreeMap(getStore(), getM()));
		return clone;
	}
}

class TreeSetView extends java.util.AbstractSet implements SortedSet {

	private SortedMap m; // The backing Map
	private Set keySet;  // The keySet view of the backing Map

	TreeSet outer;

	TreeSetView(TreeSet outer, SortedMap m) {
		this.outer = outer;
		this.m = m;
		keySet = m.keySet();
	}

	public Iterator iterator() {
		return keySet.iterator();
	}

	public int size() {
		return m.size();
	}

	public boolean isEmpty() {
		return m.isEmpty();
	}

	public boolean contains(Object o) {
		return m.containsKey(o);
	}

	public boolean add(Object o) {
		return m.put(o, outer.PRESENT())==null;
	}

	public boolean remove(Object o) {
		return m.remove(o)==outer.PRESENT();
	}

	public void clear() {
		m.clear();
	}

	public SortedSet subSet(Object fromElement, Object toElement) {
		return new TreeSetView(outer, m.subMap(fromElement, toElement));
	}

	public SortedSet headSet(Object toElement) {
		return new TreeSetView(outer, m.headMap(toElement));
	}

	public SortedSet tailSet(Object fromElement) {
		return new TreeSetView(outer, m.tailMap(fromElement));
	}

	public Comparator comparator() {
		return m.comparator();
	}

	public Object first() {
		return m.firstKey();
	}

	public Object last() {
		return m.lastKey();
	}
}
