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
import persistence.Secondary;
import persistence.Store;

public class TreeSet<E> extends AbstractSet<E> implements SortedSet<E>, Cloneable {
	public TreeSet() {
	}

	public TreeSet(final Store store, SortedMap<E,Object> map) {
		super(store);
		setM(map);
	}

	public TreeSet(final Store store) {
		this(store, new TreeMap<E,Object>(store));
	}

	public TreeSet(final Store store, Comparator<? super E> c) {
		this(store, new TreeMap<E,Object>(store, c));
	}

	public TreeSet(final Store store, Collection<? extends E> c) {
		this(store);
		addAll(c);		
	}

	public TreeSet(final Store store, SortedSet<E> s) {
		this(store, s.comparator());
		addAll(s);
	}

	protected PersistentClass createClass() {
		return getClass() == TreeSet.class?new TreeSetClass(this):super.createClass();
	}

	SortedMap<E,Object> m() {
		return getM();
	}

	Set<E> keySet() {
		return m().keySet();
	}

	public SortedMap<E,Object> getM() {
		return get("m");
	}

	public void setM(SortedMap<E,Object> map) {
		set("m",map);
	}

	public Iterator<E> iterator() {
		return keySet().iterator();
	}

	public int size() {
		return m().size();
	}

	@Secondary
	public boolean isEmpty() {
		return m().isEmpty();
	}

	public boolean contains(Object o) {
		return m().containsKey(o);
	}

	Object PRESENT() {
		return ((TreeSetClass)getStore().get(TreeSet.class)).PRESENT();
	}

	public boolean add(E e) {
		return m().put(e, PRESENT())==null;
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
//			SortedSet<E> set = (SortedSet<E>)c;
//			TreeMap<E,Object> map = (TreeMap<E,Object>)m();
//			Comparator<? super E> cc = set.comparator();
//			Comparator<? super E> mc = map.comparator();
//			if (cc==mc || (cc != null && cc.equals(mc))) {
//				map.addAllForTreeSet(set, persistentClass().PRESENT());
//				return true;
//			}
//		}
//		return super.addAll(c);
//	}

	public SortedSet<E> subSet(E fromElement, E toElement) {
		return new TreeSetView<>(this, m().subMap(fromElement, toElement));
	}

	public SortedSet<E> headSet(E toElement) {
		return new TreeSetView<>(this, m().headMap(toElement));
	}

	public SortedSet<E> tailSet(E fromElement) {
		return new TreeSetView<>(this, m().tailMap(fromElement));
	}

	public Comparator<? super E> comparator() {
		return m().comparator();
	}

	public E first() {
		return m().firstKey();
	}

	public E last() {
		return m().lastKey();
	}

	@SuppressWarnings("unchecked")
	public synchronized PersistentObject clone() {
			TreeSet<E> clone;
			try {
				clone = (TreeSet<E>)super.clone();
			} catch (final CloneNotSupportedException e) {
				throw new InternalError();
			}
			clone.setM(new TreeMap<>(getStore(), getM()));
			return clone;
	}
}

class TreeSetView<E> extends java.util.AbstractSet<E> implements SortedSet<E> {

	private SortedMap<E,Object> m; // The backing Map
	private Set<E> keySet;  // The keySet view of the backing Map

	TreeSet<E> outer;

	TreeSetView(TreeSet<E> outer, SortedMap<E,Object> m) {
		this.outer = outer;
		this.m = m;
		keySet = m.keySet();
	}

	public Iterator<E> iterator() {
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

	public boolean add(E e) {
		return m.put(e, outer.PRESENT())==null;
	}

	public boolean remove(Object o) {
		return m.remove(o)==outer.PRESENT();
	}

	public void clear() {
		m.clear();
	}

	public SortedSet<E> subSet(E fromElement, E toElement) {
		return new TreeSetView<>(outer, m.subMap(fromElement, toElement));
	}

	public SortedSet<E> headSet(E toElement) {
		return new TreeSetView<>(outer, m.headMap(toElement));
	}

	public SortedSet<E> tailSet(E fromElement) {
		return new TreeSetView<>(outer, m.tailMap(fromElement));
	}

	public Comparator<? super E> comparator() {
		return m.comparator();
	}

	public E first() {
		return m.firstKey();
	}

	public E last() {
		return m.lastKey();
	}
}
