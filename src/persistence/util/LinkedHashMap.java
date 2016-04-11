/*
 * @(#)LinkedHashMap.java	1.11 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import persistence.Array;
import persistence.Store;

public class LinkedHashMap<K,V> extends HashMap<K,V> {
	public LinkedHashMap() {
	}

	public LinkedHashMap(final Store store, int initialCapacity, float loadFactor) {
		super(store, initialCapacity, loadFactor);
	}

	public LinkedHashMap(final Store store, int initialCapacity) {
		super(store, initialCapacity);
	}

	public LinkedHashMap(final Store store) {
		super(store);
	}

	public LinkedHashMap(final Store store, final Map<? extends K, ? extends V> m) {
		super(store, m);
	}

	public LinkedHashMap(final Store store, int initialCapacity, float loadFactor, boolean accessOrder) {
		super(store, initialCapacity, loadFactor);
		setAccessOrder(accessOrder);
	}

	public Entry<K,V> getHeader() {
		return get("header");
	}

	public void setHeader(Entry<K,V> entry) {
		set("header",entry);
	}

	public boolean isAccessOrder() {
		return get("accessOrder");
	}

	public void setAccessOrder(boolean b) {
		set("accessOrder",b);
	}

	void init() {
		setHeader(new Entry<K,V>(getStore(), -1, null, null, null));
		getHeader().setBefore(getHeader());
		getHeader().setAfter(getHeader());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void transfer(Array<HashMap.Entry> newTable) {
		int newCapacity = newTable.length();
		for (Entry<K,V> e = getHeader().getAfter(); e != getHeader(); e = e.getAfter()) {
			int index = indexFor(e.getHash(), newCapacity);
			e.setNext(newTable.get(index));
			newTable.set(index,e);
		}
	}

	@SuppressWarnings("rawtypes")
	public synchronized boolean containsValue(Object value) {
		// Overridden to take advantage of faster iterator
		if (value==null) {
			for (Entry e = getHeader().getAfter(); e != getHeader(); e = e.getAfter())
				if (e.getValue()==null)
					return true;
		} else {
			for (Entry e = getHeader().getAfter(); e != getHeader(); e = e.getAfter())
				if (value.equals(e.getValue()))
					return true;
		}
		return false;
	}

	public synchronized V get(Object key) {
		Entry<K,V> e = (Entry<K,V>)getEntry(key);
		if (e == null)
			return null;
		e.recordAccess(LinkedHashMap.this);
		return e.getValue();
	}

	public V put(K key, V value) {
		HashMap.Entry<K,V> e = getEntry(key);
		if(e != null) return put(e,value);
		else {
			Map.Entry<K,V> entry = putMapping(key,value);
			return (entry == null ? null : entry.getValue());
		}
	}

	synchronized V put(HashMap.Entry<K,V> entry, V value) {
		V oldValue = entry.getValue();
		entry.setValue(value);
		entry.recordAccess(LinkedHashMap.this);
		return oldValue;
	}

	public void clear() {
		super.clear();
		getHeader().setBefore(getHeader());
		getHeader().setAfter(getHeader());
	}

	public static class Entry<K,V> extends HashMap.Entry<K,V> {
		public Entry() {
		}

		public Entry(final Store store, final int hash, final K key, final V value, final HashMap.Entry<K,V> next) {
			super(store, hash, key, value, next);
		}

		public Entry<K,V> getBefore() {
			return get("before");
		}

		public void setBefore(Entry<K,V> entry) {
			set("before",entry);
		}

		public Entry<K,V> getAfter() {
			return get("after");
		}

		public void setAfter(Entry<K,V> entry) {
			set("after",entry);
		}

		private void remove() {
			getBefore().setAfter(getAfter());
			getAfter().setBefore(getBefore());
		}

		private void addBefore(Entry<K,V> existingEntry) {
			setAfter(existingEntry);
			setBefore(existingEntry.getBefore());
			getBefore().setAfter(this);
			getAfter().setBefore(this);
		}

		void recordAccess(HashMap<K,V> m) {
			LinkedHashMap<K,V> lm = (LinkedHashMap<K,V>)m;
			if (lm.isAccessOrder()) {
				lm.setModCount(lm.getModCount()+1);
				remove();
				addBefore(lm.getHeader());
			}
		}

		void recordRemoval(HashMap<K,V> m) {
			remove();
		}
	}

	private abstract class LinkedHashIterator<T> implements Iterator<T> {
		Entry<K,V> nextEntry = getHeader().getAfter();
		Entry<K,V> lastReturned = null;

		int expectedModCount = modCount();

		public boolean hasNext() {
			return nextEntry != getHeader();
		}

		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();

			LinkedHashMap.this.remove(lastReturned.getKey());
			lastReturned = null;
			expectedModCount = modCount();
		}

		Entry<K,V> nextEntry() {
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			if (nextEntry == getHeader())
				throw new NoSuchElementException();

			Entry<K,V> e = lastReturned = nextEntry;
			nextEntry = e.getAfter();
			return e;
		}
	}

	private class KeyIterator extends LinkedHashIterator<K> {
		public K next() { return nextEntry().getKey(); }
	}

	private class ValueIterator extends LinkedHashIterator<V> {
		public V next() { return nextEntry().getValue(); }
	}

	private class EntryIterator extends LinkedHashIterator<Map.Entry<K,V>> {
		public Map.Entry<K,V> next() { return nextEntry(); }
	}

	Iterator<K> newKeyIterator()   { return new KeyIterator();   }
	Iterator<V> newValueIterator() { return new ValueIterator(); }
	Iterator<Map.Entry<K,V>> newEntryIterator() { return new EntryIterator(); }

	HashMap.Entry<K,V> addEntry(int hash, K key, V value, int bucketIndex) {
		HashMap.Entry<K,V> entry=createEntry(hash, key, value, bucketIndex);

		Entry<K,V> eldest = getHeader().getAfter();
		if (removeEldestEntry(eldest)) {
			removeEntryForKey(eldest.getKey());
		} else {
			if (getSize() >= getThreshold()) 
				resize(2 * getTable().length());
		}
		return entry;
	}

	@SuppressWarnings("unchecked")
	HashMap.Entry<K,V> createEntry(int hash, K key, V value, int bucketIndex) {
		Entry<K,V> e = new Entry<>(getStore(), hash, key, value, getTable().get(bucketIndex));
		getTable().set(bucketIndex,e);
		e.addBefore(getHeader());
		setSize(getSize()+1);
		return e;
	}

	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
		return false;
	}
}
