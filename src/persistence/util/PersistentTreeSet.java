/*
 * @(#)TreeSet.java		1.26 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import persistence.PersistentClass;
import persistence.PersistentObject;

public class PersistentTreeSet extends PersistentAbstractSet
					 implements SortedSet, Cloneable
{
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentAbstractSet.Accessor {
		public Accessor() throws RemoteException {}

		public void init(SortedMap map) {
			setM(map);
		}

		public SortedMap m() {
			return getM();
		}

		public synchronized PersistentObject persistentClone() {
			PersistentTreeSet clone = (PersistentTreeSet)super.persistentClone();
			clone.setM((SortedMap)create(PersistentTreeMap.class,new Class[] {SortedMap.class},new Object[] {getM()}));
			return clone;
		}
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(TreeSetClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	SortedMap m() {
		return (SortedMap)execute(
			new MethodCall("m",new Class[] {},new Object[] {}));
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

	void init(SortedMap map) {
		execute(
			new MethodCall("init",new Class[] {SortedMap.class},new Object[] {map}));
	}

	public void init() {
		init((SortedMap)create(PersistentTreeMap.class));
	}

	public void init(Comparator c) {
		init((SortedMap)create(PersistentTreeMap.class,new Class[] {Comparator.class},new Object[] {c}));
	}

	public void init(Collection c) {
		init();
		addAll(c);		
	}

	public void init(SortedSet s) {
		init(s.comparator());
		addAll(s);
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

	public boolean add(Object o) {
		return m().put(o, ((TreeSetClass)persistentClass()).PRESENT())==null;
	}

	public boolean remove(Object o) {
		return m().remove(o)==((TreeSetClass)persistentClass()).PRESENT();
	}

	public void clear() {
		m().clear();
	}

	public boolean addAll(Collection c) {
		// Use linear-time version if applicable
		if (m().size()==0 && c.size() > 0 && c instanceof SortedSet && 
			m() instanceof PersistentTreeMap) {
			SortedSet set = (SortedSet)c;
			PersistentTreeMap map = (PersistentTreeMap)m();
			Comparator cc = set.comparator();
			Comparator mc = map.comparator();
			if (cc==mc || (cc != null && cc.equals(mc))) {
				map.addAllForTreeSet(set, ((TreeSetClass)persistentClass()).PRESENT());
				return true;
			}
		}
		return super.addAll(c);
	}

	public SortedSet subSet(Object fromElement, Object toElement) {
		return (SortedSet)create(PersistentTreeSet.class,new Class[] {SortedMap.class},new Object[] {m().subMap(fromElement, toElement)});
	}

	public SortedSet headSet(Object toElement) {
		return (SortedSet)create(PersistentTreeSet.class,new Class[] {SortedMap.class},new Object[] {m().headMap(toElement)});
	}

	public SortedSet tailSet(Object fromElement) {
		return (SortedSet)create(PersistentTreeSet.class,new Class[] {SortedMap.class},new Object[] {m().tailMap(fromElement)});
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
}
