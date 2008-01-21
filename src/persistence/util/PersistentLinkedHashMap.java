/*
 * @(#)LinkedHashMap.java	1.11 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import persistence.Array;
import persistence.PersistentObject;

public class PersistentLinkedHashMap extends PersistentHashMap {
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentHashMap.Accessor {
		public Accessor() throws RemoteException {}

		public void init(int initialCapacity, float loadFactor,
							 boolean accessOrder) {
			PersistentLinkedHashMap.super.init(initialCapacity,loadFactor);
			setAccessOrder(accessOrder);
		}

		public synchronized Object get(Object key) {
			Entry e = (Entry)getEntry(key);
			if (e == null)
				return null;
			e.recordAccess(PersistentLinkedHashMap.this);
			return e.getValue();
		}

		public synchronized Object put(Entry entry, Object value) {
			Object oldValue = entry.getValue();
			entry.setValue(value);
			entry.recordAccess(PersistentLinkedHashMap.this);
			return oldValue;
		}
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

	public void init(int initialCapacity, float loadFactor) {
		super.init(initialCapacity, loadFactor);
	}

	public void init(int initialCapacity) {
		super.init(initialCapacity);
	}

	public void init() {
		super.init();
	}

	public void init(Map m) {
		super.init(m);
	}

	public void init(int initialCapacity, float loadFactor, boolean accessOrder) {
		execute(
			new MethodCall("init",new Class[] {int.class,float.class,boolean.class},new Object[] {new Integer(initialCapacity),new Float(loadFactor),new Boolean(accessOrder)}));
	}

	void init0() {
		setHeader((Entry)create(Entry.class,new Class[] {int.class,Object.class,Object.class,PersistentHashMap.Entry.class},new Object[] {new Integer(-1), null, null, null}));
		getHeader().setBefore(getHeader());
		getHeader().setAfter(getHeader());
	}

	void transfer(Array newTable) {
		int newCapacity = newTable.length();
		for (Entry e = getHeader().getAfter(); e != getHeader(); e = e.getAfter()) {
			int index = indexFor(e.getHash(), newCapacity);
			e.setNext((PersistentHashMap.Entry)newTable.get(index));
			newTable.set(index,e);
		}
	}

	public boolean containsValue(Object value) {
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

	public Object get(Object key) {
		return execute(
			new MethodCall("get",new Class[] {Object.class},new Object[] {key}));
	}

	public Object put(Object key, Object value) {
		PersistentHashMap.Entry e = getEntry(key);
		if(e != null) return put(e,value);
		else return putMapping(key,value);
	}

	Object put(Entry entry, Object value) {
		return execute(
			new MethodCall("put",new Class[] {Map.Entry.class,Object.class},new Object[] {entry,value}),
			new MethodCall("put",new Class[] {Map.Entry.class,Object.class},new Object[] {entry,null}),1);
	}

	public void clear() {
		super.clear();
		getHeader().setBefore(getHeader());
		getHeader().setAfter(getHeader());
	}

	public static class Entry extends PersistentHashMap.Entry {
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

		public void init(int hash, Object key, Object value, PersistentHashMap.Entry next) {
			super.init(hash, key, value, next);
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

		void recordAccess(PersistentHashMap m) {
			PersistentLinkedHashMap lm = (PersistentLinkedHashMap)m;
			if (lm.isAccessOrder()) {
				lm.setModCount(lm.getModCount()+1);
				remove();
				addBefore(lm.getHeader());
			}
		}

		void recordRemoval(PersistentHashMap m) {
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

			PersistentLinkedHashMap.this.remove(lastReturned.getKey());
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

	PersistentHashMap.Entry addEntry(int hash, Object key, Object value, int bucketIndex) {
		PersistentHashMap.Entry entry=createEntry(hash, key, value, bucketIndex);

		Entry eldest = getHeader().getAfter();
		if (removeEldestEntry(eldest)) {
			removeEntryForKey(eldest.getKey());
		} else {
			if (getSize() >= getThreshold()) 
				resize(2 * getTable().length());
		}
		return entry;
	}

	PersistentHashMap.Entry createEntry(int hash, Object key, Object value, int bucketIndex) {
		Entry e = (Entry)create(Entry.class,new Class[] {int.class,Object.class,Object.class,PersistentHashMap.Entry.class},new Object[] {new Integer(hash), key, value, getTable().get(bucketIndex)});
		getTable().set(bucketIndex,e);
		e.addBefore(getHeader());
		setSize(getSize()+1);
		return e;
	}

	protected boolean removeEldestEntry(Map.Entry eldest) {
		return false;
	}
}
