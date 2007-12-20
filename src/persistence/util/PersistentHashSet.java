/*
 * @(#)HashSet.java		1.28 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import persistence.PersistentClass;
import persistence.PersistentObject;

public class PersistentHashSet extends PersistentAbstractSet
					 implements Set, Cloneable
{
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentAbstractSet.Accessor {
		public Accessor() throws RemoteException {}

		public void init(PersistentHashMap map) {
			setMap(map);
		}

		public PersistentHashMap map() {
			return getMap();
		}

		public PersistentObject remoteClone() {
			PersistentHashSet newSet = (PersistentHashSet)super.remoteClone();
			newSet.setMap((PersistentHashMap)getMap().clone());
			return newSet;
		}
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(HashSetClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	PersistentHashMap map() {
		return (PersistentHashMap)execute(
			new MethodCall("map",new Class[] {},new Object[] {}));
	}

	public PersistentHashMap getMap() {
		return (PersistentHashMap)get("map");
	}

	public void setMap(PersistentHashMap map) {
		set("map",map);
	}

	void init(PersistentHashMap map) {
		execute(
			new MethodCall("init",new Class[] {PersistentHashMap.class},new Object[] {map}));
	}

	public void init() {
		init((PersistentHashMap)create(PersistentHashMap.class));
	}

	public void init(Collection c) {
		init((PersistentHashMap)create(PersistentHashMap.class,new Class[] {int.class},new Object[] {new Integer(Math.max((int) (c.size()/.75f) + 1, 16))}));
		addAll(c);
	}

	public void init(int initialCapacity, float loadFactor) {
		init((PersistentHashMap)create(PersistentHashMap.class,new Class[] {int.class,float.class},new Object[] {new Integer(initialCapacity),new Float(loadFactor)}));
	}

	public void init(int initialCapacity) {
		init((PersistentHashMap)create(PersistentHashMap.class,new Class[] {int.class},new Object[] {new Integer(initialCapacity)}));
	}

//	PersistentHashSet(int initialCapacity, float loadFactor, boolean dummy) {
//		init(create(PersistentLinkedHashMap.class,new Class[] {int.class,float.class},new Object[] {new Integer(initialCapacity),new Float(loadFactor)}));
//	}

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
		return map().put(o, ((HashSetClass)persistentClass()).PRESENT())==null;
	}

	public boolean remove(Object o) {
		return map().remove(o)==((HashSetClass)persistentClass()).PRESENT();
	}

	public void clear() {
		map().clear();
	}
}
