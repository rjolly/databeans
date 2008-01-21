/*
 * @(#)TreeMap.java		1.56 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import persistence.PersistentObject;

public class PersistentTreeMap extends PersistentAbstractMap
					 implements SortedMap, Cloneable
{
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentAbstractMap.Accessor {
		public Accessor() throws RemoteException {}

		public void init(Comparator c) {
			setComparator(c);
		}

		public void init(SortedMap m) {
			comparator = m.comparator();
			try {
				buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
			} catch (java.io.IOException cannotHappen) {
			} catch (ClassNotFoundException cannotHappen) {
			}
		}

		public int size() {
			return getSize();
		}

		public synchronized boolean containsValue(Object value) {
			return (getRoot()==null ? false :
				(value==null ? valueSearchNull(getRoot())
				: valueSearchNonNull(getRoot(), value)));
		}

		public Comparator comparator() {
			return getComparator();
		}

		public synchronized Entry getEntry(Object key) {
			Entry p = getRoot();
			while (p != null) {
				int cmp = compare(key,p.getKey0());
				if (cmp == 0)
					return p;
				else if (cmp < 0)
					p = p.getLeft();
				else
					p = p.getRight();
			}
			return null;
		}

		public synchronized Entry getCeilEntry(Object key) {
			Entry p = getRoot();
			if (p==null)
				return null;

			while (true) {
				int cmp = compare(key, p.getKey0());
				if (cmp == 0) {
					return p;
				} else if (cmp < 0) {
					if (p.getLeft() != null)
						p = p.getLeft();
					else
						return p;
				} else {
					if (p.getRight() != null) {
						p = p.getRight();
					} else {
						Entry parent = p.getParent();
						Entry ch = p;
						while (parent != null && ch == parent.getRight()) {
							ch = parent;
							parent = parent.getParent();
						}
						return parent;
					}
				}
			}
		}

		public synchronized Entry getPrecedingEntry(Object key) {
			Entry p = getRoot();
			if (p==null)
				return null;

			while (true) {
				int cmp = compare(key, p.getKey0());
				if (cmp > 0) {
					if (p.getRight() != null)
						p = p.getRight();
					else
						return p;
				} else {
					if (p.getLeft() != null) {
						p = p.getLeft();
					} else {
						Entry parent = p.getParent();
						Entry ch = p;
						while (parent != null && ch == parent.getLeft()) {
							ch = parent;
							parent = parent.getParent();
						}
						return parent;
					}
				}
			}
		}

		Object put0(Object key, Object value) {
			Entry t = getRoot();

			if (t == null) {
				incrementSize();
				setRoot((Entry)create(Entry.class,new Class[] {Object.class,Object.class,Entry.class},new Object[] {key, value, null}));
				return ((AbstractMapClass)persistentClass()).NULL();
			}

			while (true) {
				int cmp = compare(key, t.getKey0());
				if (cmp == 0) {
					return t.setValue(value);
				} else if (cmp < 0) {
					if (t.getLeft() != null) {
						t = t.getLeft();
					} else {
						incrementSize();
						t.setLeft((Entry)create(Entry.class,new Class[] {Object.class,Object.class,Entry.class},new Object[] {key, value, t}));
						fixAfterInsertion(t.getLeft());
						return ((AbstractMapClass)persistentClass()).NULL();
					}
				} else { // cmp > 0
					if (t.getRight() != null) {
						t = t.getRight();
					} else {
						incrementSize();
						t.setRight((Entry)create(Entry.class,new Class[] {Object.class,Object.class,Entry.class},new Object[] {key, value, t}));
						fixAfterInsertion(t.getRight());
						return ((AbstractMapClass)persistentClass()).NULL();
					}
				}
			}
		}

		Object remove0(Object key) {
			Entry p = getEntry(key);
			if (p == null)
				return ((AbstractMapClass)persistentClass()).NULL();
			
			Object oldValue = p.getValue0();
			deleteEntry(p);
			return oldValue;
		}

		public synchronized PersistentObject persistentClone() {
			PersistentTreeMap clone = (PersistentTreeMap)super.persistentClone();

			// Put clone into "virgin" state (except for comparator)
			clone.setRoot(null);
			clone.setSize(0);
			clone.setModCount(0);

			// Initialize clone with our mappings
			try {
				clone.buildFromSorted(getSize(), entrySet().iterator(), null, null);
			} catch (java.io.IOException cannotHappen) {
			} catch (ClassNotFoundException cannotHappen) {
			}

			return clone;
		}

		public synchronized boolean hasChildren(Entry entry) {
			return entry.getLeft() != null && entry.getRight() != null;
		}

		public synchronized Entry firstEntry() {
			Entry p = getRoot();
			if (p != null)
				while (p.getLeft() != null)
					p = p.getLeft();
			return p;
		}

		public synchronized Entry lastEntry() {
			Entry p = getRoot();
			if (p != null)
				while (p.getRight() != null)
					p = p.getRight();
			return p;
		}

		public synchronized Entry successor(Entry t) {
			if (t == null)
				return null;
			else if (t.getRight() != null) {
				Entry p = t.getRight();
				while (p.getLeft() != null)
					p = p.getLeft();
				return p;
			} else {
				Entry p = t.getParent();
				Entry ch = t;
				while (p != null && ch == p.getRight()) {
					ch = p;
					p = p.getParent();
				}
				return p;
			}
		}

		public int modCount() {
			return getModCount();
		}
	}

	public Comparator getComparator() {
		return (Comparator)get("comparator");
	}

	public void setComparator(Comparator comparator) {
		set("comparator",comparator);
	}

	public Entry getRoot() {
		return (Entry)get("root");
	}

	public void setRoot(Entry entry) {
		set("root",entry);
	}

	public int getSize() {
		return ((Integer)get("size")).intValue();
	}

	public void setSize(int n) {
		set("size",new Integer(n));
	}

	public int getModCount() {
		return ((Integer)get("modCount")).intValue();
	}

	public void setModCount(int n) {
		set("modCount",new Integer(n));
	}

	private void incrementSize()   { setModCount(getModCount()+1); setSize(getSize()+1); }
	private void decrementSize()   { setModCount(getModCount()+1); setSize(getSize()-1); }

	public void init() {
	}

	public void init(Comparator c) {
		execute(
			new MethodCall("init",new Class[] {Comparator.class},new Object[] {c}));
	}

	public void init(Map m) {
		putAll(m);
	}

	public void init(SortedMap m) {
		execute(
			new MethodCall("init",new Class[] {SortedMap.class},new Object[] {m}));
	}

	// Query Operations

	public int size() {
		return ((Integer)execute(
			new MethodCall("size",new Class[] {},new Object[] {}))).intValue();
	}

	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	public boolean containsValue(Object value) {
		return ((Boolean)execute(
			new MethodCall("containsValue",new Class[] {Object.class},new Object[] {value}))).booleanValue();
	}

	private boolean valueSearchNull(Entry n) {
		if (n.getValue0() == null)
			return true;

		// Check left and right subtrees for value
		return (n.getLeft()  != null && valueSearchNull(n.getLeft())) ||
			   (n.getRight() != null && valueSearchNull(n.getRight()));
	}

	private boolean valueSearchNonNull(Entry n, Object value) {
		// Check this node for the value
		if (value.equals(n.getValue0()))
			return true;

		// Check left and right subtrees for value
		return (n.getLeft() != null && valueSearchNonNull(n.getLeft(), value)) ||
			(n.getRight() != null && valueSearchNonNull(n.getRight(), value));
	}

	public Object get(Object key) {
		Entry p = getEntry(key);
		return (p==null ? null : p.getValue());
	}

	transient Comparator comparator;

	public Comparator comparator() {
		return comparator==null?comparator=(Comparator)execute(
			new MethodCall("comparator",new Class[] {},new Object[] {})):comparator;
	}

	public Object firstKey() {
		return key(firstEntry());
	}

	public Object lastKey() {
		return key(lastEntry());
	}

//	public void putAll(Map map) {
//		int mapSize = map.size();
//		if (size==0 && mapSize!=0 && map instanceof SortedMap) {
//			Comparator c = ((SortedMap)map).comparator();
//			if (c == comparator || (c != null && c.equals(comparator))) {
//			  ++modCount;
//			  try {
//				  buildFromSorted(mapSize, map.entrySet().iterator(),
//								  null, null);
//			  } catch (java.io.IOException cannotHappen) {
//			  } catch (ClassNotFoundException cannotHappen) {
//			  }
//			  return;
//			}
//		}
//		super.putAll(map);
//	}

	private Entry getEntry(Object key) {
		return (Entry)execute(
			new MethodCall("getEntry",new Class[] {Object.class},new Object[] {key}));
	}

	private Entry getCeilEntry(Object key) {
		return (Entry)execute(
			new MethodCall("getCeilEntry",new Class[] {Object.class},new Object[] {key}));
	}

	private Entry getPrecedingEntry(Object key) {
		return (Entry)execute(
			new MethodCall("getPrecedingEntry",new Class[] {Object.class},new Object[] {key}));
	}

	private static Object key(Entry e) {
		if (e==null)
			throw new NoSuchElementException();
		return e.getKey();
	}

//	public void clear() {
//		setModCount(getModCount()+1);
//		setSize(0);
//		setRoot(null);
//	}

	// Views

	private transient volatile Set entrySet = null;

	public Set keySet() {
		if (keySet == null) {
			keySet = new AbstractSet() {
				public Iterator iterator() {
					return new KeyIterator();
				}

				public int size() {
					return PersistentTreeMap.this.size();
				}

				public boolean contains(Object o) {
					return containsKey(o);
				}

				public boolean remove(Object o) {
					int oldSize = PersistentTreeMap.this.size();
					PersistentTreeMap.this.remove(o);
					return PersistentTreeMap.this.size() != oldSize;
				}

//				public void clear() {
//					PersistentTreeMap.this.clear();
//				}
			};
		}
		return keySet;
	}

	public Collection values() {
		if (values == null) {
			values = new AbstractCollection() {
				public Iterator iterator() {
					return new ValueIterator();
				}

				public int size() {
					return PersistentTreeMap.this.size();
				}

				public boolean contains(Object o) {
					for (Entry e = firstEntry(); e != null; e = successor(e))
						if (valEquals(e.getValue(), o))
							return true;
					return false;
				}

				public boolean remove(Object o) {
					for (Entry e = firstEntry(); e != null; e = successor(e)) {
						if (valEquals(e.getValue(), o)) {
							deleteEntry(e);
							return true;
						}
					}
					return false;
				}

//				public void clear() {
//					PersistentTreeMap.this.clear();
//				}
			};
		}
		return values;
	}

	public Set entrySet() {
		if (entrySet == null) {
			entrySet = new AbstractSet() {
				public Iterator iterator() {
					return new EntryIterator();
				}

				public boolean contains(Object o) {
					if (!(o instanceof Map.Entry))
						return false;
					Map.Entry entry = (Map.Entry)o;
					Object value = entry.getValue();
					Entry p = getEntry(entry.getKey());
					return p != null && valEquals(p.getValue(), value);
				}

				public boolean remove(Object o) {
					if (!(o instanceof Map.Entry))
						return false;
					Map.Entry entry = (Map.Entry)o;
					Object value = entry.getValue();
					Entry p = getEntry(entry.getKey());
					if (p != null && valEquals(p.getValue(), value)) {
						deleteEntry(p);
						return true;
					}
					return false;
				}

				public int size() {
					return PersistentTreeMap.this.size();
				}

//				public void clear() {
//					PersistentTreeMap.this.clear();
//				}
			};
		}
		return entrySet;
	}

	public SortedMap subMap(Object fromKey, Object toKey) {
		return new SubMap(fromKey, toKey);
	}

	public SortedMap headMap(Object toKey) {
		return new SubMap(toKey, true);
	}

	public SortedMap tailMap(Object fromKey) {
		return new SubMap(fromKey, false);
	}

	int modCount() {
		return ((Integer)execute(
			new MethodCall("modCount",new Class[] {},new Object[] {}))).intValue();
	}

	private class SubMap extends AbstractMap
				implements SortedMap {

		private boolean fromStart = false, toEnd = false;
		private Object  fromKey,	   toKey;

		SubMap(Object fromKey, Object toKey) {
			if (compare(fromKey, toKey) > 0)
				throw new IllegalArgumentException("fromKey > toKey");
			this.fromKey = fromKey;
			this.toKey = toKey;
		}

		SubMap(Object key, boolean headMap) {
			compare(key, key); // Type-check key

			if (headMap) {
				fromStart = true;
				toKey = key;
			} else {
				toEnd = true;
				fromKey = key;
			}
		}

		SubMap(boolean fromStart, Object fromKey, boolean toEnd, Object toKey){
			this.fromStart = fromStart;
			this.fromKey= fromKey;
			this.toEnd = toEnd;
			this.toKey = toKey;
		}

		public boolean isEmpty() {
			return entrySet.isEmpty();
		}

		public boolean containsKey(Object key) {
			return inRange(key) && PersistentTreeMap.this.containsKey(key);
		}

		public Object get(Object key) {
			if (!inRange(key))
				return null;
			return PersistentTreeMap.this.get(key);
		}

		public Object put(Object key, Object value) {
			if (!inRange(key))
				throw new IllegalArgumentException("key out of range");
			return PersistentTreeMap.this.put(key, value);
		}

		public Comparator comparator() {
			return PersistentTreeMap.this.comparator();
		}

		public Object firstKey() {
			Object first = key(fromStart ? firstEntry():getCeilEntry(fromKey));
			if (!toEnd && compare(first, toKey) >= 0)
				throw(new NoSuchElementException());
			return first;
		}

		public Object lastKey() {
			Object last = key(toEnd ? lastEntry() : getPrecedingEntry(toKey));
			if (!fromStart && compare(last, fromKey) < 0)
				throw(new NoSuchElementException());
			return last;
		}

		private transient Set entrySet = new EntrySetView();

		public Set entrySet() {
			return entrySet;
		}

		private class EntrySetView extends AbstractSet {
			private transient int size = -1, sizeModCount;

			public int size() {
				if (size == -1 || sizeModCount != modCount()) {
					size = 0;  sizeModCount = modCount();
					Iterator i = iterator();
					while (i.hasNext()) {
						size++;
						i.next();
					}
				}
				return size;
			}

			public boolean isEmpty() {
				return !iterator().hasNext();
			}

			public boolean contains(Object o) {
				if (!(o instanceof Map.Entry))
					return false;
				Map.Entry entry = (Map.Entry)o;
				Object key = entry.getKey();
				if (!inRange(key))
					return false;
				PersistentTreeMap.Entry node = getEntry(key);
				return node != null &&
					   valEquals(node.getValue(), entry.getValue());
			}

			public boolean remove(Object o) {
				if (!(o instanceof Map.Entry))
					return false;
				Map.Entry entry = (Map.Entry)o;
				Object key = entry.getKey();
				if (!inRange(key))
					return false;
				PersistentTreeMap.Entry node = getEntry(key);
				if (node!=null && valEquals(node.getValue(),entry.getValue())){
					deleteEntry(node);
					return true;
				}
				return false;
			}

			public Iterator iterator() {
				return new SubMapEntryIterator(
					(fromStart ? firstEntry() : getCeilEntry(fromKey)),
					(toEnd	   ? null	  : getCeilEntry(toKey)));
			}
		}

		public SortedMap subMap(Object fromKey, Object toKey) {
			if (!inRange2(fromKey))
				throw new IllegalArgumentException("fromKey out of range");
			if (!inRange2(toKey))
				throw new IllegalArgumentException("toKey out of range");
			return new SubMap(fromKey, toKey);
		}

		public SortedMap headMap(Object toKey) {
			if (!inRange2(toKey))
				throw new IllegalArgumentException("toKey out of range");
			return new SubMap(fromStart, fromKey, false, toKey);
		}

		public SortedMap tailMap(Object fromKey) {
			if (!inRange2(fromKey))
				throw new IllegalArgumentException("fromKey out of range");
			return new SubMap(false, fromKey, toEnd, toKey);
		}

		private boolean inRange(Object key) {
			return (fromStart || compare(key, fromKey) >= 0) &&
				   (toEnd	 || compare(key, toKey)   <  0);
		}

		// This form allows the high endpoint (as well as all legit keys)
		private boolean inRange2(Object key) {
			return (fromStart || compare(key, fromKey) >= 0) &&
				   (toEnd	 || compare(key, toKey)   <= 0);
		}
	}

	private class EntryIterator implements Iterator {
		private int expectedModCount = modCount();
		private Entry lastReturned = null;
		Entry next;

		EntryIterator() {
			next = firstEntry();
		}

		// Used by SubMapEntryIterator
		EntryIterator(Entry first) {
			next = first;
		}

		public boolean hasNext() {
			return next != null;
		}

		final Entry nextEntry() {
			if (next == null)
				throw new NoSuchElementException();
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			lastReturned = next;
			next = successor(next);
			return lastReturned;
		}

		public Object next() {
			return nextEntry();
		}

		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			if (hasChildren(lastReturned))
				next = lastReturned;
			deleteEntry(lastReturned);
			expectedModCount++;
			lastReturned = null;
		}
	}

	boolean hasChildren(Entry entry) {
		return ((Boolean)execute(
			new MethodCall("hasChildren",new Class[] {Entry.class},new Object[] {entry}))).booleanValue();
	}

	private class KeyIterator extends EntryIterator {
		public Object next() {
			return nextEntry().getKey();
		}
	}

	private class ValueIterator extends EntryIterator {
		public Object next() {
			return nextEntry().getValue();
		}
	}

	private class SubMapEntryIterator extends EntryIterator {
		private final Object firstExcludedKey;

		SubMapEntryIterator(Entry first, Entry firstExcluded) {
			super(first);
			firstExcludedKey = (firstExcluded == null ?
								firstExcluded : firstExcluded.getKey());
		}

		public boolean hasNext() {
			return next != null && next.getKey() != firstExcludedKey;
		}

		public Object next() {
			if (next == null || next.getKey() == firstExcludedKey)
				throw new NoSuchElementException();
			return nextEntry();
		}
	}

	private int compare(Object k1, Object k2) {
		return (comparator()==null ? ((Comparable)k1).compareTo(k2)
								 : comparator().compare(k1, k2));
	}

	private static boolean valEquals(Object o1, Object o2) {
		return (o1==null ? o2==null : o1.equals(o2));
	}

	private static final boolean RED   = false;
	private static final boolean BLACK = true;

	public static class Entry extends PersistentObject implements Map.Entry {
		protected PersistentObject.Accessor createAccessor() throws RemoteException {
			return new Accessor();
		}

		protected class Accessor extends PersistentObject.Accessor {
			public Accessor() throws RemoteException {}

			public void init(Object key, Object value, Entry parent) {
				setKey0(key);
				setValue0(value);
				setParent(parent);
				setColor(BLACK);
			}

			public Object getKey() {
				return getKey0();
			}

			public Object getValue() {
				return getValue0();
			}

			public Object setValue(Object value) {
				Object oldValue = getValue0();
				setValue0(value);
				return oldValue;
			}

			public boolean persistentEquals(Object o) {
				if (!(o instanceof Map.Entry))
					return false;
				Map.Entry e = (Map.Entry)o;
				
				return valEquals(getKey0(),e.getKey()) && valEquals(getValue0(),e.getValue());
			}

			public int persistentHashCode() {
				int keyHash = (getKey0()==null ? 0 : getKey0().hashCode());
				int valueHash = (getValue0()==null ? 0 : getValue0().hashCode());
				return keyHash ^ valueHash;
			}

			public String persistentToString() {
				return getKey0() + "=" + getValue0();
			}
		}

		Object getKey0() {
			return get("key");
		}

		void setKey0(Object obj) {
			set("key",obj);
		}

		Object getValue0() {
			return get("value");
		}

		void setValue0(Object obj) {
			set("value",obj);
		}

		public Entry getLeft() {
			return (Entry)get("left");
		}

		public void setLeft(Entry entry) {
			set("left",entry);
		}

		public Entry getRight() {
			return (Entry)get("right");
		}

		public void setRight(Entry entry) {
			set("right",entry);
		}

		public Entry getParent() {
			return (Entry)get("parent");
		}

		public void setParent(Entry entry) {
			set("parent",entry);
		}

		public boolean getColor() {
			return ((Boolean)get("color")).booleanValue();
		}

		public void setColor(boolean b) {
			set("color",new Boolean(b));
		}

		public void init(Object key, Object value, Entry parent) { 
			execute(
				new MethodCall("init",new Class[] {Object.class,Object.class,Entry.class},new Object[] {key,value,parent}));
		}

		public Object getKey() {
			return execute(
				new MethodCall("getKey",new Class[] {},new Object[] {}));
		}

		public Object getValue() {
			return execute(
				new MethodCall("getValue",new Class[] {},new Object[] {}));
		}
	
		public Object setValue(Object value) {
			return execute(
				new MethodCall("setValue",new Class[] {Object.class},new Object[] {value}),
				new MethodCall("setValue",new Class[] {Object.class},new Object[] {null}),0);
		}
	}

	private Entry firstEntry() {
		return (Entry)execute(
			new MethodCall("firstEntry",new Class[] {},new Object[] {}));
	}

	private Entry lastEntry() {
		return (Entry)execute(
			new MethodCall("lastEntry",new Class[] {},new Object[] {}));
	}

	private Entry successor(Entry t) {
		return (Entry)execute(
			new MethodCall("successor",new Class[] {Entry.class},new Object[] {t}));
	}

	private static boolean colorOf(Entry p) {
		return (p == null ? BLACK : p.getColor());
	}

	private static Entry parentOf(Entry p) {
		return (p == null ? null: p.getParent());
	}

	private static void setColor(Entry p, boolean c) {
		if (p != null) p.setColor(c);
	}

	private static Entry leftOf(Entry p) {
		return (p == null)? null: p.getLeft();
	}

	private static Entry rightOf(Entry p) {
		return (p == null)? null: p.getRight();
	}

	/** From CLR **/
	private void rotateLeft(Entry p) {
		Entry r = p.getRight();
		p.setRight(r.getLeft());
		if (r.getLeft() != null)
			r.getLeft().setParent(p);
		r.setParent(p.getParent());
		if (p.getParent() == null)
			setRoot(r);
		else if (p.getParent().getLeft() == p)
			p.getParent().setLeft(r);
		else
			p.getParent().setRight(r);
		r.setLeft(p);
		p.setParent(r);
	}

	/** From CLR **/
	private void rotateRight(Entry p) {
		Entry l = p.getLeft();
		p.setLeft(l.getRight());
		if (l.getRight() != null) l.getRight().setParent(p);
		l.setParent(p.getParent());
		if (p.getParent() == null)
			setRoot(l);
		else if (p.getParent().getRight() == p)
			p.getParent().setRight(l);
		else p.getParent().setLeft(l);
		l.setRight(p);
		p.setParent(l);
	}


	/** From CLR **/
	private void fixAfterInsertion(Entry x) {
		x.setColor(RED);

		while (x != null && x != getRoot() && x.getParent().getColor() == RED) {
			if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
				Entry y = rightOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == rightOf(parentOf(x))) {
						x = parentOf(x);
						rotateLeft(x);
					}
					setColor(parentOf(x), BLACK);
					setColor(parentOf(parentOf(x)), RED);
					if (parentOf(parentOf(x)) != null) 
						rotateRight(parentOf(parentOf(x)));
				}
			} else {
				Entry y = leftOf(parentOf(parentOf(x)));
				if (colorOf(y) == RED) {
					setColor(parentOf(x), BLACK);
					setColor(y, BLACK);
					setColor(parentOf(parentOf(x)), RED);
					x = parentOf(parentOf(x));
				} else {
					if (x == leftOf(parentOf(x))) {
						x = parentOf(x);
						rotateRight(x);
					}
					setColor(parentOf(x),  BLACK);
					setColor(parentOf(parentOf(x)), RED);
					if (parentOf(parentOf(x)) != null) 
						rotateLeft(parentOf(parentOf(x)));
				}
			}
		}
		getRoot().setColor(BLACK);
	}

	private void deleteEntry(Entry p) {
		decrementSize();

		// If strictly internal, copy successor's element to p and then make p
		// point to successor.
		if (p.getLeft() != null && p.getRight() != null) {
			Entry s = successor (p);
			p.setKey0(s.getKey0());
			p.setValue0(s.getValue0());
			p = s;
		} // p has 2 children

		// Start fixup at replacement node, if it exists.
		Entry replacement = (p.getLeft() != null ? p.getLeft() : p.getRight());

		if (replacement != null) {
			// Link replacement to parent
			replacement.setParent(p.getParent());
			if (p.getParent() == null)
				setRoot(replacement);
			else if (p == p.getParent().getLeft())
				p.getParent().setLeft(replacement);
			else
				p.getParent().setRight(replacement);

			// Null out links so they are OK to use by fixAfterDeletion.
			p.setLeft(null);
			p.setRight(null);
			p.setParent(null);

			// Fix replacement
			if (p.getColor() == BLACK)
				fixAfterDeletion(replacement);
		} else if (p.getParent() == null) { // return if we are the only node.
			setRoot(null);
		} else { //  No children. Use self as phantom replacement and unlink.
			if (p.getColor() == BLACK)
				fixAfterDeletion(p);

			if (p.getParent() != null) {
				if (p == p.getParent().getLeft())
					p.getParent().setLeft(null);
				else if (p == p.getParent().getRight())
					p.getParent().setRight(null);
				p.setParent(null);
			}
		}
	}

	/** From CLR **/
	private void fixAfterDeletion(Entry x) {
		while (x != getRoot() && colorOf(x) == BLACK) {
			if (x == leftOf(parentOf(x))) {
				Entry sib = rightOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateLeft(parentOf(x));
					sib = rightOf(parentOf(x));
				}

				if (colorOf(leftOf(sib))  == BLACK && 
					colorOf(rightOf(sib)) == BLACK) {
					setColor(sib,  RED);
					x = parentOf(x);
				} else {
					if (colorOf(rightOf(sib)) == BLACK) {
						setColor(leftOf(sib), BLACK);
						setColor(sib, RED);
						rotateRight(sib);
						sib = rightOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(rightOf(sib), BLACK);
					rotateLeft(parentOf(x));
					x = getRoot();
				}
			} else { // symmetric
				Entry sib = leftOf(parentOf(x));

				if (colorOf(sib) == RED) {
					setColor(sib, BLACK);
					setColor(parentOf(x), RED);
					rotateRight(parentOf(x));
					sib = leftOf(parentOf(x));
				}

				if (colorOf(rightOf(sib)) == BLACK && 
					colorOf(leftOf(sib)) == BLACK) {
					setColor(sib,  RED);
					x = parentOf(x);
				} else {
					if (colorOf(leftOf(sib)) == BLACK) {
						setColor(rightOf(sib), BLACK);
						setColor(sib, RED);
						rotateLeft(sib);
						sib = leftOf(parentOf(x));
					}
					setColor(sib, colorOf(parentOf(x)));
					setColor(parentOf(x), BLACK);
					setColor(leftOf(sib), BLACK);
					rotateRight(parentOf(x));
					x = getRoot();
				}
			}
		}

		setColor(x, BLACK); 
	}

	/** Intended to be called only from TreeSet.addAll **/
	void addAllForTreeSet(SortedSet set, Object defaultVal) {
	  try {
		  buildFromSorted(set.size(), set.iterator(), null, defaultVal);
	  } catch (java.io.IOException cannotHappen) {
	  } catch (ClassNotFoundException cannotHappen) {
	  }
	}

	private void buildFromSorted(int size, Iterator it,
								  java.io.ObjectInputStream str,
								  Object defaultVal)
		throws  java.io.IOException, ClassNotFoundException {
		setSize(size);
		setRoot(buildFromSorted(0, 0, size-1, computeRedLevel(size),
			it, str, defaultVal));
	}

	private Entry buildFromSorted(int level, int lo, int hi,
					     int redLevel,
					     Iterator it, 
					     java.io.ObjectInputStream str,
					     Object defaultVal) 
		throws  java.io.IOException, ClassNotFoundException {

		if (hi < lo) return null;

		int mid = (lo + hi) / 2;
		
		Entry left  = null;
		if (lo < mid) 
			left = buildFromSorted(level+1, lo, mid - 1, redLevel,
								   it, str, defaultVal);
		
		// extract key and/or value from iterator or stream
		Object key;
		Object value;
		if (it != null) { // use iterator
			if (defaultVal==null) {
				Map.Entry entry = (Map.Entry) it.next();
				key = entry.getKey();
				value = entry.getValue();
			} else {
				key = it.next();
				value = defaultVal;
			}
		} else { // use stream
			key = str.readObject();
			value = (defaultVal != null ? defaultVal : str.readObject());
		}

		Entry middle = (Entry)create(Entry.class,new Class[] {Object.class,Object.class,Entry.class},new Object[] {key, value, null});
		
		// color nodes in non-full bottommost level red
		if (level == redLevel)
			middle.setColor(RED);
		
		if (left != null) { 
			middle.setLeft(left);
			left.setParent(middle);
		}
		
		if (mid < hi) {
			Entry right = buildFromSorted(level+1, mid+1, hi, redLevel,
				it, str, defaultVal);
			middle.setRight(right);
			right.setParent(middle);
		}
		
		return middle;
	}

	private static int computeRedLevel(int sz) {
		int level = 0;
		for (int m = sz - 1; m >= 0; m = m / 2 - 1) 
			level++;
		return level;
	}
}
