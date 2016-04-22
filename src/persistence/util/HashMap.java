/*
 * @(#)HashMap.java		1.59 04/12/09
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import persistence.Array;
import persistence.PersistentArray;
import persistence.PersistentClass;
import persistence.PersistentObject;
import persistence.Store;

public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable {
	static final int DEFAULT_INITIAL_CAPACITY = 16;
	static final int MAXIMUM_CAPACITY = 1 << 30;
	static final float DEFAULT_LOAD_FACTOR = 0.75f;

	public HashMap() {
	}

	public HashMap(final Store store, int initialCapacity, float loadFactor) {
		super(store);
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal initial capacity: " +
				initialCapacity);
		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new IllegalArgumentException("Illegal load factor: " +
				loadFactor);

		// Find a power of 2 >= initialCapacity
		int capacity = 1;
		while (capacity < initialCapacity)
			capacity <<= 1;

		setLoadFactor(loadFactor);
		setThreshold((int)(capacity * loadFactor));
		setTable(new PersistentArray<>(store, Entry.class, capacity));
		init();
	}

	public HashMap(final Store store, int initialCapacity) {
		this(store, initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public HashMap(final Store store) {
		super(store);
		setLoadFactor(DEFAULT_LOAD_FACTOR);
		setThreshold((int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR));
		setTable(new PersistentArray<>(store, Entry.class, DEFAULT_INITIAL_CAPACITY));
		init();
	}

	void init() {
	}

	public HashMap(final Store store, final Map<? extends K, ? extends V> m) {
		this(store, Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
		putAllForCreate(m);
	}

	protected PersistentClass createClass() {
		return getClass() == HashMap.class?new HashMapClass<>(this):super.createClass();
	}

	@SuppressWarnings("rawtypes")
	public Array<Entry> getTable() {
		return get("table");
	}

	@SuppressWarnings("rawtypes")
	public void setTable(Array<Entry> array) {
		set("table",array);
	}

	public int getSize() {
		return get("size");
	}

	public void setSize(int n) {
		set("size",n);
	}

	public int getThreshold() {
		return get("threshold");
	}

	public void setThreshold(int n) {
		set("threshold",n);
	}

	public float getLoadFactor() {
		return get("loadFactor");
	}

	public void setLoadFactor(float f) {
		set("loadFactor",f);
	}

	public int getModCount() {
		return get("modCount");
	}

	public void setModCount(int n) {
		set("modCount",n);
	}

	// internal utilities

	V NULL() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	K NULL_KEY() {
		return ((HashMapClass<K,V>)getStore().get(HashMap.class)).NULL_KEY();
	}

	K maskNull(K key) {
		return (key == null ? NULL_KEY() : key);
	}

	static int hash(Object x) {
		int h = x.hashCode();

		h += ~(h << 9);
		h ^=  (h >>> 14);
		h +=  (h << 4);
		h ^=  (h >>> 10);
		return h;
	}

	static boolean eq(Object x, Object y) {
		return x == y || x.equals(y);
	}

	static int indexFor(int h, int length) {
		return h & (length-1);
	}
 
	public int size() {
		return getSize();
	}
  
//	public boolean isEmpty() {
//		return getSize() == 0;
//	}

	public V get(Object key) {
		Entry<K,V> e=getEntry(key);
		return e==null?null:e.getValue();
	}

	@SuppressWarnings("unchecked")
	public synchronized boolean containsKey(Object key) {
		K k = maskNull((K)key);
		int hash = hash(k);
		int i = indexFor(hash, getTable().length());
		Entry<K,V> e = getTable().get(i);
		while (e != null) {
			if (e.getHash() == hash && eq(k, e.getKey()))
				return true;
			e=e.getNext();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	synchronized Entry<K,V> getEntry(Object key) {
		K k = maskNull((K)key);
		int hash = hash(k);
		int i = indexFor(hash, getTable().length());
		Entry<K,V> e = getTable().get(i);
		while (e != null && !(e.getHash() == hash && eq(k, e.getKey0())))
			e=e.getNext();
		return e;
	}

	public V put(K key, V value) {
		Entry<K,V> e = getEntry(key);
		if(e != null) {
			V oldValue = e.getValue();
			e.setValue(value);
			e.recordAccess(this);
			return oldValue;
		} else return putMapping(key,value).getValue();
	}

	@SuppressWarnings("unchecked")
	private void putForCreate(K key, V value) {
		K k = maskNull(key);
		int hash = hash(k);
		int i = indexFor(hash, getTable().length());

		for (Entry<K,V> e = getTable().get(i); e != null; e = e.getNext()) {
			if (e.getHash() == hash && eq(k, e.getKey())) {
				e.setValue(value);
				return;
			}
		}

		createEntry(hash, k, value, i);
	}

	void putAllForCreate(Map<? extends K, ? extends V> m) {
		for (Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<? extends K, ? extends V> e = i.next();
			putForCreate(e.getKey(), e.getValue());
		}
	}

	@SuppressWarnings("rawtypes")
	void resize(int newCapacity) {
		Array<Entry> oldTable = getTable();
		int oldCapacity = oldTable.length();
		if (oldCapacity == MAXIMUM_CAPACITY) {
			setThreshold(Integer.MAX_VALUE);
			return;
		}

		Array<Entry> newTable = new PersistentArray<>(getStore(), Entry.class, newCapacity);
		transfer(newTable);
		setTable(newTable);
		setThreshold((int)(newCapacity * getLoadFactor()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void transfer(Array<Entry> newTable) {
		Array<Entry> src = getTable();
		int newCapacity = newTable.length();
		for (int j = 0; j < src.length(); j++) {
			Entry<K,V> e = src.get(j);
			if (e != null) {
				src.set(j,null);
				do {
					Entry<K,V> next = e.getNext();
					int i = indexFor(e.getHash(), newCapacity);  
					e.setNext(newTable.get(i));
					newTable.set(i,e);
					e = next;
				} while (e != null);
			}
		}
	}

//	public void putAll(Map m) {
//		int numKeysToBeAdded = m.size();
//		if (numKeysToBeAdded == 0)
//			return;
//
//		if (numKeysToBeAdded > getThreshold()) {
//			int targetCapacity = (int)(numKeysToBeAdded / getLoadFactor() + 1);
//			if (targetCapacity > MAXIMUM_CAPACITY)
//				targetCapacity = MAXIMUM_CAPACITY;
//			int newCapacity = getTable().length();
//			while (newCapacity < targetCapacity)
//				newCapacity <<= 1;
//			if (newCapacity > getTable().length())
//				resize(newCapacity);
//		}
//
//		for (Iterator<Map.Entry<K,V>> i = m.entrySet().iterator(); i.hasNext(); ) {
//			Map.Entry<K,V> e = i.next();
//			put(e.getKey(), e.getValue());
//		}
//	}
  
	public V remove(Object key) {
		Entry<K,V> e = removeEntryForKey(key);
		return (e == null ? null : e.getValue());
	}

	Entry<K,V> removeEntryForKey(Object key) {
		return (Entry<K,V>)removeMapping(getEntry(key));
	}

	Map.Entry<K,V> putMapping(Map.Entry<K,V> entry) {
		return putMapping(entry.getKey(),entry.getValue());
	}

	public synchronized Map.Entry<K,V> putMapping(K key, V value) {
		K k = maskNull(key);
		int hash = hash(k);
		int i = indexFor(hash, getTable().length());
		
		setModCount(getModCount()+1);
		return addEntry(hash, k, value, i);
	}

	@SuppressWarnings("unchecked")
	synchronized Map.Entry<K,V> removeMapping(Map.Entry<K,V> entry) {
		K k = maskNull(entry.getKey());
		int hash = hash(k);
		int i = indexFor(hash, getTable().length());
		Entry<K,V> prev = getTable().get(i);
		Entry<K,V> e = prev;
		
		while (e != null) {
			Entry<K,V> next = e.getNext();
			if (e.getHash() == hash && e.equals(entry)) {
				setModCount(getModCount()+1);
				setSize(getSize()-1);
				if (prev == e)
					getTable().set(i,next);
				else
					prev.setNext(next);
				e.recordRemoval(HashMap.this);
				return e;
			}
			prev = e;
			e = next;
		}
		
		return e;
	}

//	public void clear() {
//		setModCount(getModCount()+1);
//		Array<Entry> tab = getTable();
//		for (int i = 0; i < tab.length(); i++) 
//			tab.set(i,null);
//		setSize(0);
//	}

	@SuppressWarnings("rawtypes")
	public synchronized boolean containsValue(Object value) {
		if (value == null)
			return containsNullValue();
		
		Array<Entry> tab = getTable();
		for (int i = 0; i < tab.length() ; i++)
			for (Entry e = tab.get(i) ; e != null ; e = e.getNext())
				if (value.equals(e.getValue()))
					return true;
		return false;
	}

	@SuppressWarnings("rawtypes")
	private boolean containsNullValue() {
		Array<Entry> tab = getTable();
		for (int i = 0; i < tab.length() ; i++)
			for (Entry e = tab.get(i) ; e != null ; e = e.getNext())
				if (e.getValue() == null)
					return true;
		return false;
	}

	public static class Entry<K,V> extends PersistentObject implements Map.Entry<K,V> {
		public Entry() {
		}

		public Entry(final Store store, final int h, final K k, final V v, final Entry<K,V> n) {
			super(store);
			setValue0(v);
			setNext(n);
			setKey0(k);
			setHash(h);
		}

		@SuppressWarnings("unchecked")
		K NULL_KEY() {
			return ((HashMapClass<K,V>)getStore().get(HashMap.class)).NULL_KEY();
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

		public int getHash() {
			return get("hash");
		}

		public void setHash(int n) {
			set("hash",n);
		}

		public Entry<K,V> getNext() {
			return get("next");
		}

		public void setNext(Entry<K,V> entry) {
			set("next",entry);
		}

		K unmaskNull(K key) {
			return (key == NULL_KEY() ? null : key);
		}

		public K getKey() {
			return unmaskNull(getKey0());
		}

		public V getValue() {
			return getValue0();
		}
	
		public V setValue(V newValue) {
			V oldValue = getValue0();
			setValue0(newValue);
			return oldValue;
		}

		void recordAccess(HashMap<K,V> m) {}

		void recordRemoval(HashMap<K,V> m) {}

		@SuppressWarnings("rawtypes")
		public boolean equals(PersistentObject o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry)o;
			Object k1 = getKey0();
			Object k2 = e.getKey();
			if (k1 == k2 || (k1 != null && k1.equals(k2))) {
				Object v1 = getValue0();
				Object v2 = e.getValue();
				if (v1 == v2 || (v1 != null && v1.equals(v2)))
					return true;
			}
			return false;
		}

		public int hashCode() {
			return (getKey0()==NULL_KEY() ? 0 : getKey0().hashCode()) ^
				(getValue0()==null  ? 0 : getValue0().hashCode());
		}

		public String toString() {
			return getKey0() + "=" + getValue0();
		}
	}

	Entry<K,V> addEntry(int hash, K key, V value, int bucketIndex) {
		Entry<K,V> entry=createEntry(hash,key,value,bucketIndex);
		if (getSize() >= getThreshold()) 
			resize(2 * getTable().length());
		return entry;
	}

	@SuppressWarnings("unchecked")
	Entry<K,V> createEntry(int hash, K key, V value, int bucketIndex) {
		Entry<K,V> entry;
		getTable().set(bucketIndex,entry=new Entry<>(getStore(), hash, key, value, getTable().get(bucketIndex)));
		setSize(getSize()+1);
		return entry;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	synchronized Entry<K,V> nextEntry(Entry<K,V> entry) {
		Entry<K,V> n;
		Array<Entry> t = getTable();
		if(entry == null) {
			n = entry;
			int i = t.length();
			if (getSize() != 0) {
				while (n == null && i > 0) n = t.get(--i);
			}
		} else {
			n = entry.getNext();
			Object k = maskNull(entry.getKey0());
			int hash = hash(k);
			int i = indexFor(hash, t.length());
			while (n == null && i > 0) n = t.get(--i);
		}
		return n;
	}

	@SuppressWarnings("unchecked")
	public synchronized PersistentObject clone() {
		HashMap<K, V> result;
		try {
			result = (HashMap<K,V>)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
		result.setTable(new PersistentArray<>(getStore(), Entry.class, getTable().length()));
		result.setModCount(0);
		result.setSize(0);
		result.init();
		result.putAllForCreate(HashMap.this);
		return result;
	}

	int modCount() {
		return getModCount();
	}

	private abstract class HashIterator<E> implements Iterator<E> {
		Entry<K,V> next;			// next entry to return
		int expectedModCount;		// For fast-fail 
		Entry<K,V> current;			// current entry

		HashIterator() {
			expectedModCount = modCount();
			next = HashMap.this.nextEntry(null);
		}

		public boolean hasNext() {
			return next != null;
		}

		Entry<K,V> nextEntry() { 
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			Entry<K,V> e = next;
			if (e == null) 
				throw new NoSuchElementException();

			next = HashMap.this.nextEntry(e);
			return current = e;
		}

		public void remove() {
			if (current == null)
				throw new IllegalStateException();
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			Object k = current.getKey();
			current = null;
			HashMap.this.removeEntryForKey(k);
			expectedModCount = modCount();
		}

	}

	private class ValueIterator extends HashIterator<V> {
		public V next() {
			return nextEntry().getValue();
		}
	}

	private class KeyIterator extends HashIterator<K> {
		public K next() {
			return nextEntry().getKey();
		}
	}

	private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
		public Map.Entry<K,V> next() {
			return nextEntry();
		}
	}

	// Subclass overrides these to alter behavior of views' iterator() method
	Iterator<K> newKeyIterator()   {
		return new KeyIterator();
	}
	Iterator<V> newValueIterator()   {
		return new ValueIterator();
	}
	Iterator<Map.Entry<K,V>> newEntryIterator()   {
		return new EntryIterator();
	}

	// Views

	private transient Set<Map.Entry<K,V>> entrySet = null;

	public Set<K> keySet() {
		Set<K> ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet()));
	}

	private class KeySet extends java.util.AbstractSet<K> {
		public Iterator<K> iterator() {
			return newKeyIterator();
		}
		public int size() {
			return HashMap.this.size();
		}
		public boolean contains(Object o) {
			return containsKey(o);
		}
		public boolean remove(Object o) {
			return HashMap.this.removeEntryForKey(o) != null;
		}
//		public void clear() {
//			HashMap.this.clear();
//		}
	}

	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null ? vs : (values = new Values()));
	}

	private class Values extends java.util.AbstractCollection<V> {
		public Iterator<V> iterator() {
			return newValueIterator();
		}
		public int size() {
			return HashMap.this.size();
		}
		public boolean contains(Object o) {
			return containsValue(o);
		}
//		public void clear() {
//			HashMap.this.clear();
//		}
	}

	public Set<Map.Entry<K,V>> entrySet() {
		Set<Map.Entry<K,V>> es = entrySet;
		return (es != null ? es : (entrySet = new EntrySet()));
	}

	private class EntrySet extends java.util.AbstractSet<Map.Entry<K,V>> {
		public Iterator<Map.Entry<K,V>> iterator() {
			return newEntryIterator();
		}
		@SuppressWarnings("unchecked")
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<K,V> e = (Map.Entry<K,V>)o;
			Entry<K,V> candidate = getEntry(e.getKey());
			return candidate != null && candidate.equals(e);
		}
		@SuppressWarnings("unchecked")
		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			
			Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
			return removeMapping(entry) != null;
		}
		public int size() {
			return HashMap.this.size();
		}
//		public void clear() {
//			HashMap.this.clear();
//		}
	}
}
