/*
 * @(#)TreeMap.java		1.56 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import persistence.PersistentClass;
import persistence.PersistentObject;
import persistence.Store;

public class TreeMap<K,V> extends AbstractMap<K,V> implements SortedMap<K,V>, Cloneable {
	public TreeMap() {
	}

	public TreeMap(final Store store) {
		super(store);
	}

	public TreeMap(final Store store, final Comparator<? super K> c) {
		super(store);
		setComparator(c);
	}

	public TreeMap(final Store store, final Map<? extends K, ? extends V> m) {
		super(store);
		putAll(m);
	}

	public TreeMap(final Store store, final SortedMap<K, ? extends V> m) {
		super(store);
		comparator = m.comparator();
		try {
			buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
		} catch (java.io.IOException cannotHappen) {
		} catch (ClassNotFoundException cannotHappen) {
		}
	}

	protected PersistentClass createClass() {
		return getClass() == TreeMap.class?new AbstractMapClass<>(this):super.createClass();
	}

	public Comparator<? super K> getComparator() {
		return get("comparator");
	}

	public void setComparator(Comparator<? super K> comparator) {
		set("comparator",comparator);
	}

	public Entry<K,V> getRoot() {
		return get("root");
	}

	public void setRoot(Entry<K,V> entry) {
		set("root",entry);
	}

	public int getSize() {
		return get("size");
	}

	public void setSize(int n) {
		set("size",n);
	}

	public int getModCount() {
		return ((Integer)get("modCount")).intValue();
	}

	public void setModCount(int n) {
		set("modCount",new Integer(n));
	}

	private void incrementSize()   { setModCount(getModCount()+1); setSize(getSize()+1); }
	private void decrementSize()   { setModCount(getModCount()+1); setSize(getSize()-1); }

	// Query Operations

	public int size() {
		return getSize();
	}

	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	public synchronized boolean containsValue(Object value) {
		return (getRoot()==null ? false :
			(value==null ? valueSearchNull(getRoot())
			: valueSearchNonNull(getRoot(), value)));
	}

	private boolean valueSearchNull(Entry<K,V> n) {
		if (n.getValue0() == null)
			return true;

		// Check left and right subtrees for value
		return (n.getLeft()  != null && valueSearchNull(n.getLeft())) ||
			   (n.getRight() != null && valueSearchNull(n.getRight()));
	}

	private boolean valueSearchNonNull(Entry<K,V> n, Object value) {
		// Check this node for the value
		if (value.equals(n.getValue0()))
			return true;

		// Check left and right subtrees for value
		return (n.getLeft() != null && valueSearchNonNull(n.getLeft(), value)) ||
			(n.getRight() != null && valueSearchNonNull(n.getRight(), value));
	}

	public V get(Object key) {
		Entry<K,V> p = getEntry(key);
		return (p==null ? null : p.getValue());
	}

	transient Comparator<? super K> comparator;

	public Comparator<? super K> comparator() {
		if (comparator == null) {
			comparator=getComparator();
		}
		return comparator;
	}

	public K firstKey() {
		return key(firstEntry());
	}

	public K lastKey() {
		return key(lastEntry());
	}

//	public void putAll(Map<? extends K, ? extends V> map) {
//		int mapSize = map.size();
//		if (size==0 && mapSize!=0 && map instanceof SortedMap) {
//			Comparator<? super K> c = ((SortedMap<K,V)map).comparator();
//			if (c == comparator || (c != null && c.equals(comparator))) {
//				setModCount(getModCount()+1);
//			  try {
//				buildFromSorted(mapSize, map.entrySet().iterator(),
//					null, null);
//			  } catch (java.io.IOException cannotHappen) {
//			  } catch (ClassNotFoundException cannotHappen) {
//			  }
//			  return;
//			}
//		}
//		super.putAll(map);
//	}

	synchronized Entry<K,V> getEntry(Object key) {
		Entry<K,V> p = getRoot();
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

	synchronized Entry<K,V> getCeilEntry(K key) {
		Entry<K,V> p = getRoot();
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
					Entry<K,V> parent = p.getParent();
					Entry<K,V> ch = p;
					while (parent != null && ch == parent.getRight()) {
						ch = parent;
						parent = parent.getParent();
					}
					return parent;
				}
			}
		}
	}

	synchronized Entry<K,V> getPrecedingEntry(K key) {
		Entry<K,V> p = getRoot();
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
					Entry<K,V> parent = p.getParent();
					Entry<K,V> ch = p;
					while (parent != null && ch == parent.getLeft()) {
						ch = parent;
						parent = parent.getParent();
					}
					return parent;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	V NULL() {
		return ((AbstractMapClass<K,V>)getStore().get(TreeMap.class)).NULL();
	}

	V put0(K key, V value) {
		Entry<K,V> t = getRoot();

		if (t == null) {
			incrementSize();
			setRoot(new Entry<>(getStore(), key, value, null));
			return NULL();
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
					t.setLeft(new Entry<>(getStore(), key, value, t));
					fixAfterInsertion(t.getLeft());
					return NULL();
				}
			} else { // cmp > 0
				if (t.getRight() != null) {
					t = t.getRight();
				} else {
					incrementSize();
					t.setRight(new Entry<>(getStore(), key, value, t));
					fixAfterInsertion(t.getRight());
					return NULL();
				}
			}
		}
	}

	V remove0(Object key) {
		Entry<K,V> p = getEntry(key);
		if (p == null)
			return NULL();

		V oldValue = p.getValue0();
		deleteEntry(p);
		return oldValue;
	}

	static <K> K key(Entry<K,?> e) {
		if (e==null) throw new NoSuchElementException();
		return e.getKey();
	}

//	public void clear() {
//		setModCount(getModCount()+1);
//		setSize(0);
//		setRoot(null);
//	}

	@SuppressWarnings("unchecked")
	public synchronized PersistentObject clone() {
		TreeMap<K, V> clone;
		try {
			clone = (TreeMap<K,V>)super.clone();
		} catch (final CloneNotSupportedException e) {
			throw new InternalError();
		}

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

	// Views

	private transient volatile Set<Map.Entry<K,V>> entrySet = null;

	public Set<K> keySet() {
		if (keySet == null) {
			keySet = new java.util.AbstractSet<K>() {
				public Iterator<K> iterator() {
					return new KeyIterator();
				}

				public int size() {
					return TreeMap.this.size();
				}

				public boolean contains(Object o) {
					return containsKey(o);
				}

				public boolean remove(Object o) {
					int oldSize = TreeMap.this.size();
					TreeMap.this.remove(o);
					return TreeMap.this.size() != oldSize;
				}

//				public void clear() {
//					TreeMap.this.clear();
//				}
			};
		}
		return keySet;
	}

	public Collection<V> values() {
		if (values == null) {
			values = new java.util.AbstractCollection<V>() {
				public Iterator<V> iterator() {
					return new ValueIterator();
				}

				public int size() {
					return TreeMap.this.size();
				}

				public boolean contains(Object o) {
					for (Entry<K,V> e = firstEntry(); e != null; e = successor(e))
						if (valEquals(e.getValue(), o))
							return true;
					return false;
				}

				public boolean remove(Object o) {
					for (Entry<K,V> e = firstEntry(); e != null; e = successor(e)) {
						if (valEquals(e.getValue(), o)) {
							deleteEntry(e);
							return true;
						}
					}
					return false;
				}

//				public void clear() {
//					TreeMap.this.clear();
//				}
			};
		}
		return values;
	}

	public Set<Map.Entry<K,V>> entrySet() {
		if (entrySet == null) {
			entrySet = new java.util.AbstractSet<Map.Entry<K,V>>() {
				public Iterator<Map.Entry<K,V>> iterator() {
					return new EntryIterator();
				}

				@SuppressWarnings("unchecked")
				public boolean contains(Object o) {
					if (!(o instanceof Map.Entry))
						return false;
					Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
					V value = entry.getValue();
					Entry<K,V> p = getEntry(entry.getKey());
					return p != null && valEquals(p.getValue(), value);
				}

				@SuppressWarnings("unchecked")
				public boolean remove(Object o) {
					if (!(o instanceof Map.Entry))
						return false;
					Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
					V value = entry.getValue();
					Entry<K,V> p = getEntry(entry.getKey());
					if (p != null && valEquals(p.getValue(), value)) {
						deleteEntry(p);
						return true;
					}
					return false;
				}

				public int size() {
					return TreeMap.this.size();
				}

//				public void clear() {
//					TreeMap.this.clear();
//				}
			};
		}
		return entrySet;
	}

	public SortedMap<K,V> subMap(K fromKey, K toKey) {
		return new SubMap(fromKey, toKey);
	}

	public SortedMap<K,V> headMap(K toKey) {
		return new SubMap(toKey, true);
	}

	public SortedMap<K,V> tailMap(K fromKey) {
		return new SubMap(fromKey, false);
	}

	private class SubMap extends java.util.AbstractMap<K,V> implements SortedMap<K,V> {

		private boolean fromStart = false, toEnd = false;
		private K fromKey, toKey;

		SubMap(K fromKey, K toKey) {
			if (compare(fromKey, toKey) > 0)
				throw new IllegalArgumentException("fromKey > toKey");
			this.fromKey = fromKey;
			this.toKey = toKey;
		}

		SubMap(K key, boolean headMap) {
			compare(key, key); // Type-check key

			if (headMap) {
				fromStart = true;
				toKey = key;
			} else {
				toEnd = true;
				fromKey = key;
			}
		}

		SubMap(boolean fromStart, K fromKey, boolean toEnd, K toKey){
			this.fromStart = fromStart;
			this.fromKey= fromKey;
			this.toEnd = toEnd;
			this.toKey = toKey;
		}

		public boolean isEmpty() {
			return entrySet.isEmpty();
		}

		public boolean containsKey(Object key) {
			return inRange(key) && TreeMap.this.containsKey(key);
		}

		public V get(Object key) {
			if (!inRange(key))
				return null;
			return TreeMap.this.get(key);
		}

		public V put(K key, V value) {
			if (!inRange(key))
				throw new IllegalArgumentException("key out of range");
			return TreeMap.this.put(key, value);
		}

		public Comparator<? super K> comparator() {
			return TreeMap.this.comparator();
		}

		public K firstKey() {
			K first = key(fromStart ? firstEntry():getCeilEntry(fromKey));
			if (!toEnd && compare(first, toKey) >= 0)
				throw(new NoSuchElementException());
			return first;
		}

		public K lastKey() {
			K last = key(toEnd ? lastEntry() : getPrecedingEntry(toKey));
			if (!fromStart && compare(last, fromKey) < 0)
				throw(new NoSuchElementException());
			return last;
		}

		private transient Set<Map.Entry<K,V>> entrySet = new EntrySetView();

		public Set<Map.Entry<K,V>> entrySet() {
			return entrySet;
		}

		private class EntrySetView extends java.util.AbstractSet<Map.Entry<K,V>> {
			private transient int size = -1, sizeModCount;

			public int size() {
				if (size == -1 || sizeModCount != TreeMap.this.modCount()) {
					size = 0;  sizeModCount = TreeMap.this.modCount();
					Iterator<Map.Entry<K,V>> i = iterator();
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

			@SuppressWarnings("unchecked")
			public boolean contains(Object o) {
				if (!(o instanceof Map.Entry))
					return false;
				Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
				K key = entry.getKey();
				if (!inRange(key))
					return false;
				TreeMap.Entry<K,V> node = getEntry(key);
				return node != null &&
					valEquals(node.getValue(), entry.getValue());
			}

			@SuppressWarnings("unchecked")
			public boolean remove(Object o) {
				if (!(o instanceof Map.Entry))
					return false;
				Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
				K key = entry.getKey();
				if (!inRange(key))
					return false;
				TreeMap.Entry<K,V> node = getEntry(key);
				if (node!=null && valEquals(node.getValue(),entry.getValue())){
					deleteEntry(node);
					return true;
				}
				return false;
			}

			public Iterator<Map.Entry<K,V>> iterator() {
				return new SubMapEntryIterator(
					(fromStart ? firstEntry() : getCeilEntry(fromKey)),
					(toEnd	 ? null : getCeilEntry(toKey)));
			}
		}

		public SortedMap<K,V> subMap(K fromKey, K toKey) {
			if (!inRange2(fromKey))
				throw new IllegalArgumentException("fromKey out of range");
			if (!inRange2(toKey))
				throw new IllegalArgumentException("toKey out of range");
			return new SubMap(fromKey, toKey);
		}

		public SortedMap<K,V> headMap(K toKey) {
			if (!inRange2(toKey))
				throw new IllegalArgumentException("toKey out of range");
			return new SubMap(fromStart, fromKey, false, toKey);
		}

		public SortedMap<K,V> tailMap(K fromKey) {
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

	int modCount() {
		return getModCount();
	}

	abstract class PrivateEntryIterator<T> implements Iterator<T> {
		private int expectedModCount = modCount();
		private Entry<K,V> lastReturned = null;
		Entry<K,V> next;

		PrivateEntryIterator() {
			next = firstEntry();
		}

		// Used by SubMapEntryIterator
		PrivateEntryIterator(Entry<K,V> first) {
			next = first;
		}

		public boolean hasNext() {
			return next != null;
		}

		final Entry<K,V> nextEntry() {
			if (next == null)
				throw new NoSuchElementException();
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			lastReturned = next;
			next = successor(next);
			return lastReturned;
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

	class EntryIterator extends PrivateEntryIterator<Map.Entry<K,V>> {
		EntryIterator() {
		}
		EntryIterator(Entry<K,V> first) {
			super(first);
		}
		public Map.Entry<K,V> next() {
			return nextEntry();
		}
	}

	synchronized boolean hasChildren(Entry<K,V> entry) {
		return entry.getLeft() != null && entry.getRight() != null;
	}

	private class KeyIterator extends PrivateEntryIterator<K> {
		public K next() {
			return nextEntry().getKey();
		}
	}

	private class ValueIterator extends PrivateEntryIterator<V> {
		public V next() {
			return nextEntry().getValue();
		}
	}

	class SubMapEntryIterator extends EntryIterator {
		private final Object firstExcludedKey;

		SubMapEntryIterator(Entry<K,V> first, Entry<K,V> firstExcluded) {
			super(first);
			firstExcludedKey = (firstExcluded == null ?
				firstExcluded : firstExcluded.getKey());
		}

		public boolean hasNext() {
			return next != null && next.getKey() != firstExcludedKey;
		}

		public Map.Entry<K,V> next() {
			if (next == null || next.getKey() == firstExcludedKey)
				throw new NoSuchElementException();
			return nextEntry();
		}
	}

	@SuppressWarnings("unchecked")
	int compare(Object k1, Object k2) {
		return (comparator()==null ? ((Comparable<? super K>)k1).compareTo((K)k2)
			: comparator().compare((K)k1, (K)k2));
	}

	static boolean valEquals(Object o1, Object o2) {
		return (o1==null ? o2==null : o1.equals(o2));
	}

	private static final boolean RED   = false;
	private static final boolean BLACK = true;

	public static class Entry<K,V> extends PersistentObject implements Map.Entry<K,V> {
		public Entry() {
		}

		public Entry(final Store store, final K key, final V value, final Entry<K,V> parent) {
			super(store);
			setKey0(key);
			setValue0(value);
			setParent(parent);
			setColor(BLACK);
		}

		K getKey0() {
			return get("key");
		}

		void setKey0(K obj) {
			set("key",obj);
		}

		V getValue0() {
			return get("value");
		}

		void setValue0(V obj) {
			set("value",obj);
		}

		public Entry<K,V> getLeft() {
			return get("left");
		}

		public void setLeft(Entry<K,V> entry) {
			set("left",entry);
		}

		public Entry<K,V> getRight() {
			return get("right");
		}

		public void setRight(Entry<K,V> entry) {
			set("right",entry);
		}

		public Entry<K,V> getParent() {
			return get("parent");
		}

		public void setParent(Entry<K,V> entry) {
			set("parent",entry);
		}

		public boolean getColor() {
			return get("color");
		}

		public void setColor(boolean b) {
			set("color",b);
		}

		public K getKey() {
			return getKey0();
		}

		public V getValue() {
			return getValue0();
		}
	
		public V setValue(V value) {
			V oldValue = getValue0();
			setValue0(value);
			return oldValue;
		}

		@SuppressWarnings("rawtypes")
		public boolean equals(PersistentObject o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry)o;
			
			return valEquals(getKey0(),e.getKey()) && valEquals(getValue0(),e.getValue());
		}

		public int hashCode() {
			int keyHash = (getKey0()==null ? 0 : getKey0().hashCode());
			int valueHash = (getValue0()==null ? 0 : getValue0().hashCode());
			return keyHash ^ valueHash;
		}

		public String toString() {
			return getKey0() + "=" + getValue0();
		}
	}

	synchronized Entry<K,V> firstEntry() {
		Entry<K,V> p = getRoot();
		if (p != null)
			while (p.getLeft() != null)
				p = p.getLeft();
		return p;
	}

	synchronized Entry<K,V> lastEntry() {
		Entry<K,V> p = getRoot();
		if (p != null)
			while (p.getRight() != null)
				p = p.getRight();
		return p;
	}

	private synchronized Entry<K,V> successor(Entry<K,V> t) {
		if (t == null)
			return null;
		else if (t.getRight() != null) {
			Entry<K,V> p = t.getRight();
			while (p.getLeft() != null)
				p = p.getLeft();
			return p;
		} else {
			Entry<K,V> p = t.getParent();
			Entry<K,V> ch = t;
			while (p != null && ch == p.getRight()) {
				ch = p;
				p = p.getParent();
			}
			return p;
		}
	}

	private static <K,V> boolean colorOf(Entry<K,V> p) {
		return (p == null ? BLACK : p.getColor());
	}

	private static <K,V> Entry<K,V> parentOf(Entry<K,V> p) {
		return (p == null ? null: p.getParent());
	}

	private static <K,V> void setColor(Entry<K,V> p, boolean c) {
		if (p != null) p.setColor(c);
	}

	private static <K,V> Entry<K,V> leftOf(Entry<K,V> p) {
		return (p == null)? null: p.getLeft();
	}

	private static <K,V> Entry<K,V> rightOf(Entry<K,V> p) {
		return (p == null)? null: p.getRight();
	}

	/** From CLR **/
	private void rotateLeft(Entry<K,V> p) {
		Entry<K,V> r = p.getRight();
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
	private void rotateRight(Entry<K,V> p) {
		Entry<K,V> l = p.getLeft();
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
	private void fixAfterInsertion(Entry<K,V> x) {
		x.setColor(RED);

		while (x != null && x != getRoot() && x.getParent().getColor() == RED) {
			if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
				Entry<K,V> y = rightOf(parentOf(parentOf(x)));
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
				Entry<K,V> y = leftOf(parentOf(parentOf(x)));
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

	void deleteEntry(Entry<K,V> p) {
		decrementSize();

		// If strictly internal, copy successor's element to p and then make p
		// point to successor.
		if (p.getLeft() != null && p.getRight() != null) {
			Entry<K,V> s = successor (p);
			p.setKey0(s.getKey0());
			p.setValue0(s.getValue0());
			p = s;
		} // p has 2 children

		// Start fixup at replacement node, if it exists.
		Entry<K,V> replacement = (p.getLeft() != null ? p.getLeft() : p.getRight());

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
	private void fixAfterDeletion(Entry<K,V> x) {
		while (x != getRoot() && colorOf(x) == BLACK) {
			if (x == leftOf(parentOf(x))) {
				Entry<K,V> sib = rightOf(parentOf(x));

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
				Entry<K,V> sib = leftOf(parentOf(x));

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
//	void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
//	  try {
//		  buildFromSorted(set.size(), set.iterator(), null, defaultVal);
//	  } catch (java.io.IOException cannotHappen) {
//	  } catch (ClassNotFoundException cannotHappen) {
//	  }
//	}

	@SuppressWarnings("rawtypes")
	private void buildFromSorted(int size, Iterator it, java.io.ObjectInputStream str, V defaultVal) throws java.io.IOException, ClassNotFoundException {
		setSize(size);
		setRoot(buildFromSorted(0, 0, size-1, computeRedLevel(size), it, str, defaultVal));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Entry<K,V> buildFromSorted(int level, int lo, int hi, int redLevel, Iterator it, java.io.ObjectInputStream str, V defaultVal) throws  java.io.IOException, ClassNotFoundException {

		if (hi < lo) return null;

		int mid = (lo + hi) / 2;
		
		Entry<K,V> left  = null;
		if (lo < mid) 
			left = buildFromSorted(level+1, lo, mid - 1, redLevel,
				it, str, defaultVal);
		
		// extract key and/or value from iterator or stream
		K key;
		V value;
		if (it != null) { // use iterator
			if (defaultVal==null) {
				Map.Entry<K,V> entry = (Map.Entry<K,V>)it.next();
				key = entry.getKey();
				value = entry.getValue();
			} else {
				key = (K)it.next();
				value = defaultVal;
			}
		} else { // use stream
			key = (K) str.readObject();
			value = (defaultVal != null ? defaultVal : (V) str.readObject());
		}

		Entry<K,V> middle = new Entry<>(getStore(), key, value, null);
		
		// color nodes in non-full bottommost level red
		if (level == redLevel)
			middle.setColor(RED);
		
		if (left != null) { 
			middle.setLeft(left);
			left.setParent(middle);
		}
		
		if (mid < hi) {
			Entry<K,V> right = buildFromSorted(level+1, mid+1, hi, redLevel, it, str, defaultVal);
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
