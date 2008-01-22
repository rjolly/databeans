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

public class HashSet extends AbstractSet
					 implements Set, Cloneable
{
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends AbstractSet.Accessor {
		public Accessor() throws RemoteException {}

		public void init(HashMap map) {
			setMap(map);
		}

		public HashMap map() {
			return getMap();
		}

		public synchronized PersistentObject persistentClone() {
			HashSet newSet = (HashSet)super.persistentClone();
			newSet.setMap((HashMap)getMap().clone());
			return newSet;
		}
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(HashSetClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	HashMap map() {
		return (HashMap)execute(
			new MethodCall("map",new Class[] {},new Object[] {}));
	}

	public HashMap getMap() {
		return (HashMap)get("map");
	}

	public void setMap(HashMap map) {
		set("map",map);
	}

	void init(HashMap map) {
		execute(
			new MethodCall("init",new Class[] {HashMap.class},new Object[] {map}));
	}

	public void init() {
		init((HashMap)create(HashMap.class));
	}

	public void init(Collection c) {
		init((HashMap)create(HashMap.class,new Class[] {int.class},new Object[] {new Integer(Math.max((int) (c.size()/.75f) + 1, 16))}));
		addAll(c);
	}

	public void init(int initialCapacity, float loadFactor) {
		init((HashMap)create(HashMap.class,new Class[] {int.class,float.class},new Object[] {new Integer(initialCapacity),new Float(loadFactor)}));
	}

	public void init(int initialCapacity) {
		init((HashMap)create(HashMap.class,new Class[] {int.class},new Object[] {new Integer(initialCapacity)}));
	}

	public void init(int initialCapacity, float loadFactor, boolean dummy) {
		init((HashMap)create(LinkedHashMap.class,new Class[] {int.class,float.class},new Object[] {new Integer(initialCapacity),new Float(loadFactor)}));
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
		return map().put(o, ((HashSetClass)persistentClass()).PRESENT())==null;
	}

	public boolean remove(Object o) {
		return map().remove(o)==((HashSetClass)persistentClass()).PRESENT();
	}

	public void clear() {
		map().clear();
	}
}
