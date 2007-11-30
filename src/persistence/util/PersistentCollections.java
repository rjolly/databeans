/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

public class PersistentCollections {
	private PersistentCollections() {
	}

	public static Collection localCollection(RemoteCollection c) {
		return new LocalCollection(c);
	}

	static class LocalCollection implements Collection {
		RemoteCollection c;		   // Backing Collection

		LocalCollection(RemoteCollection c) {
			if (c==null)
				throw new NullPointerException();
			this.c = c;
		}

		public int size() {
			try {
				return c.size();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean isEmpty() {
			try {
				return c.isEmpty();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean contains(Object o) {
			try {
				return c.contains(o);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object[] toArray() {
			try {
				return c.toArray();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object[] toArray(Object[] a) {
			try {
				return c.toArray(a);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Iterator iterator() {
			try {
				return new LocalIterator(c.iterator());
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean add(Object o) {
			try {
				return c.add(o);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean remove(Object o) {
			try {
				return c.remove(o);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean containsAll(Collection coll) {
			try {
				return c.containsAll(((LocalCollection)coll).c);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean addAll(Collection coll) {
			try {
				return c.addAll(((LocalCollection)coll).c);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean removeAll(Collection coll) {
			try {
				return c.removeAll(((LocalCollection)coll).c);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean retainAll(Collection coll) {
			try {
				return c.retainAll(((LocalCollection)coll).c);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public void clear() {
			try {
				c.clear();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public String toString() {
			try {
				return c.remoteToString();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static class LocalIterator implements Iterator {
		RemoteIterator i;

		LocalIterator(RemoteIterator i) {
			this.i=i;
		}

		public boolean hasNext() {
			try {
				return i.hasNext();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object next() {
			try {
				return i.next();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void remove() {
			try {
				i.remove();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static Set localSet(RemoteSet s) {
		return new LocalSet(s);
	}

	static class LocalSet extends LocalCollection
								 implements Set {
		RemoteSet s;

		LocalSet(RemoteSet s) {
			super(s);
			this.s=s;
		}
	}

	public static SortedSet localSortedSet(RemoteSortedSet s) {
		return new LocalSortedSet(s);
	}

	static class LocalSortedSet extends LocalSet
								 implements SortedSet
	{
		RemoteSortedSet ss;

		LocalSortedSet(RemoteSortedSet s) {
			super(s);
			ss = s;
		}

		public Comparator comparator() {
			try {
				return ss.comparator();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public SortedSet subSet(Object fromElement, Object toElement) {
			try {
				return localSortedSet(ss.subSet(fromElement, toElement));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public SortedSet headSet(Object toElement) {
			try {
				return localSortedSet(ss.headSet(toElement));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public SortedSet tailSet(Object fromElement) {
			try {
				return localSortedSet(ss.tailSet(fromElement));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object first() {
			try {
				return ss.first();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object last() {
			try {
				return ss.last();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static List localList(RemoteList list) {
		return new LocalList(list);
	}

	static class LocalList extends LocalCollection
									  implements List {
		RemoteList list;

		LocalList(RemoteList list) {
			super(list);
			this.list = list;
		}

		public Object get(int index) {
			try {
				return list.get(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object set(int index, Object element) {
			try {
				return list.set(index, element);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public void add(int index, Object element) {
			try {
				list.add(index, element);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object remove(int index) {
			try {
				return list.remove(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public int indexOf(Object o) {
			try {
				return list.indexOf(o);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public int lastIndexOf(Object o) {
			try {
				return list.lastIndexOf(o);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean addAll(int index, Collection c) {
			try {
				return list.addAll(index, ((LocalCollection)c).c);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public ListIterator listIterator() {
			try {
				return new LocalListIterator(list.listIterator());
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public ListIterator listIterator(int index) {
			try {
				return new LocalListIterator(list.listIterator(index));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public List subList(int fromIndex, int toIndex) {
			try {
				return localList(list.subList(fromIndex, toIndex));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static class LocalListIterator extends LocalIterator implements ListIterator {
		RemoteListIterator li;

		LocalListIterator(RemoteListIterator i) {
			super(i);
			li=i;
		}

		public boolean hasPrevious() {
			try {
				return li.hasPrevious();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object previous() {
			try {
				return li.previous();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public int nextIndex() {
			try {
				return li.nextIndex();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public int previousIndex() {
			try {
				return li.previousIndex();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void set(Object o) {
			try {
				li.set(o);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void add(Object o) {
			try {
				li.add(o);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static Map localMap(RemoteMap m) {
		return new LocalMap(m);
	}

	static class LocalMap implements Map {
		RemoteMap m;				// Backing Map

		LocalMap(RemoteMap m) {
			if (m==null)
				throw new NullPointerException();
			this.m = m;
		}

		public int size() {
			try {
				return m.size();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean isEmpty() {
			try {
				return m.isEmpty();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean containsKey(Object key) {
			try {
				return m.containsKey(key);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public boolean containsValue(Object value) {
			try {
				return m.containsValue(value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object get(Object key) {
			try {
				return m.get(key);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object put(Object key, Object value) {
			try {
				return m.put(key, value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object remove(Object key) {
			try {
				return m.remove(key);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public void putAll(Map map) {
			try {
				m.putAll(((LocalMap)map).m);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public void clear() {
			try {
				m.clear();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		private transient Set keySet = null;
		private transient Set entrySet = null;
		private transient Collection values = null;

		public Set keySet() {
			try {
				if (keySet==null)
					keySet = localSet(m.keySet());
				return keySet;
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Set entrySet() {
			try {
				if (entrySet==null)
					entrySet = localSet(m.entrySet());
				return entrySet;
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Collection values() {
			try {
				if (values==null)
					values = localCollection(m.values());
				return values;
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public String toString() {
			try {
				return m.remoteToString();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static Map.Entry localEntry(RemoteMap.Entry e) {
		return new LocalEntry(e);
	}

	static class LocalEntry implements Map.Entry {
		RemoteMap.Entry e;

		LocalEntry(RemoteMap.Entry e) {
			if (e==null)
				throw new NullPointerException();
			this.e = e;
		}

		public Object getKey() {
			try {
				return e.getKey();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object getValue() {
			try {
				return e.getValue();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object setValue(Object value) {
			try {
				return e.setValue(value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public String toString() {
			try {
				return e.remoteToString();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static SortedMap localSortedMap(RemoteSortedMap m) {
		return new LocalSortedMap(m);
	}

	static class LocalSortedMap extends LocalMap
								 implements SortedMap
	{
		RemoteSortedMap sm;

		LocalSortedMap(RemoteSortedMap m) {
			super(m);
			sm = m;
		}

		public Comparator comparator() {
			try {
				return sm.comparator();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public SortedMap subMap(Object fromKey, Object toKey) {
			try {
				return localSortedMap(sm.subMap(fromKey, toKey));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public SortedMap headMap(Object toKey) {
			try {
				return localSortedMap(sm.headMap(toKey));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public SortedMap tailMap(Object fromKey) {
			try {
				return localSortedMap(sm.tailMap(fromKey));
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object firstKey() {
			try {
				return sm.firstKey();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		public Object lastKey() {
			try {
				return sm.lastKey();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
