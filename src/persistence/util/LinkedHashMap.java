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
import persistence.PersistentClass;
import persistence.Store;

public class LinkedHashMap extends HashMap {
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

	public LinkedHashMap(final Store store, final Map m) {
		super(store, m);
	}

	public LinkedHashMap(final Store store, int initialCapacity, float loadFactor, boolean accessOrder) {
		super(store, initialCapacity, loadFactor);
		setAccessOrder(accessOrder);
	}

	protected PersistentClass createClass() {
		return new LinkedHashMapClass(getStore());
	}

	public Entry getHeader() {
		return (Entry)get("header");
	}

	public void setHeader(Entry entry) {
		set("header",entry);
	}

	public boolean isAccessOrder() {
		return ((Boolean)get("accessOrder")).booleanValue();
	}

	public void setAccessOrder(boolean b) {
		set("accessOrder",new Boolean(b));
	}

	void init() {
		setHeader(new Entry(getStore(), -1, null, null, null));
		getHeader().setBefore(getHeader());
		getHeader().setAfter(getHeader());
	}

	void transfer(Array newTable) {
		int newCapacity = newTable.length();
		for (Entry e = getHeader().getAfter(); e != getHeader(); e = e.getAfter()) {
			int index = indexFor(e.getHash(), newCapacity);
			e.setNext((HashMap.Entry)newTable.get(index));
			newTable.set(index,e);
		}
	}

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

	public synchronized Object get(Object key) {
		Entry e = (Entry)getEntry(key);
		if (e == null)
			return null;
		e.recordAccess(LinkedHashMap.this);
		return e.getValue();
	}

	public Object put(Object key, Object value) {
		HashMap.Entry e = getEntry(key);
		if(e != null) return put(e,value);
		else return putMapping(key,value);
	}

	synchronized Object put(HashMap.Entry entry, Object value) {
		Object oldValue = entry.getValue();
		entry.setValue(value);
		entry.recordAccess(LinkedHashMap.this);
		return oldValue;
	}

	public void clear() {
		super.clear();
		getHeader().setBefore(getHeader());
		getHeader().setAfter(getHeader());
	}

	public static class Entry extends HashMap.Entry {
		public Entry() {
		}

		public Entry(final Store store, final int hash, final Object key, final Object value, final HashMap.Entry next) {
			super(store, hash, key, value, next);
		}

		private LinkedHashMapClass enclosingClass() {
			return (LinkedHashMapClass)getStore().get(LinkedHashMap.class);
		}

		public Entry getBefore() {
			return (Entry)get("before");
		}

		public void setBefore(Entry entry) {
			set("before",entry);
		}

		public Entry getAfter() {
			return (Entry)get("after");
		}

		public void setAfter(Entry entry) {
			set("after",entry);
		}

		private void remove() {
			getBefore().setAfter(getAfter());
			getAfter().setBefore(getBefore());
		}

		private void addBefore(Entry existingEntry) {
			setAfter(existingEntry);
			setBefore(existingEntry.getBefore());
			getBefore().setAfter(this);
			getAfter().setBefore(this);
		}

		void recordAccess(HashMap m) {
			LinkedHashMap lm = (LinkedHashMap)m;
			if (lm.isAccessOrder()) {
				lm.setModCount(lm.getModCount()+1);
				remove();
				addBefore(lm.getHeader());
			}
		}

		void recordRemoval(HashMap m) {
			remove();
		}
	}

	private abstract class LinkedHashIterator implements Iterator {
		Entry nextEntry = getHeader().getAfter();
		Entry lastReturned = null;

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

		Entry nextEntry() {
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			if (nextEntry == getHeader())
				throw new NoSuchElementException();

			Entry e = lastReturned = nextEntry;
			nextEntry = e.getAfter();
			return e;
		}
	}

	private class KeyIterator extends LinkedHashIterator {
		public Object next() { return nextEntry().getKey(); }
	}

	private class ValueIterator extends LinkedHashIterator {
		public Object next() { return nextEntry().getValue(); }
	}

	private class EntryIterator extends LinkedHashIterator {
		public Object next() { return nextEntry(); }
	}

	Iterator newKeyIterator()   { return new KeyIterator();   }
	Iterator newValueIterator() { return new ValueIterator(); }
	Iterator newEntryIterator() { return new EntryIterator(); }

	HashMap.Entry addEntry(int hash, Object key, Object value, int bucketIndex) {
		HashMap.Entry entry=createEntry(hash, key, value, bucketIndex);

		Entry eldest = getHeader().getAfter();
		if (removeEldestEntry(eldest)) {
			removeEntryForKey(eldest.getKey());
		} else {
			if (getSize() >= getThreshold()) 
				resize(2 * getTable().length());
		}
		return entry;
	}

	HashMap.Entry createEntry(int hash, Object key, Object value, int bucketIndex) {
		Entry e = new Entry(getStore(), hash, key, value, (HashMap.Entry)getTable().get(bucketIndex));
		getTable().set(bucketIndex,e);
		e.addBefore(getHeader());
		setSize(getSize()+1);
		return e;
	}

	protected boolean removeEldestEntry(Map.Entry eldest) {
		return false;
	}
}
