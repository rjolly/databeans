/*
 * @(#)HashMap.java	1.59 04/12/09
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import persistence.*;

public class PersistentHashMap extends PersistentAbstractMap implements RemoteMap {
	public RemoteArray getTable() {
		return (RemoteArray)get("table");
	}

	public void setTable(RemoteArray array) {
		set("table",array);
	}

	public int getCount() {
		return ((Integer)get("count")).intValue();
	}

	public void setCount(int n) {
		set("count",new Integer(n));
	}

	public int getThreshold() {
		return ((Integer)get("threshold")).intValue();
	}

	public void setThreshold(int n) {
		set("threshold",new Integer(n));
	}

	public float getLoadFactor() {
		return ((Float)get("loadFactor")).floatValue();
	}

	public void setLoadFactor(float f) {
		set("loadFactor",new Float(f));
	}

	public int getModCount() {
		return ((Integer)get("modCount")).intValue();
	}

	public void setModCount(int n) {
		set("modCount",new Integer(n));
	}

	public PersistentHashMap() throws RemoteException {}

	public PersistentHashMap(Accessor accessor, Connection connection, Integer initialCapacity, Float loadFactor) throws RemoteException {
		super(accessor,connection);
		if (initialCapacity.intValue() < 0)
			throw new IllegalArgumentException("Illegal Initial Capacity: "+initialCapacity.intValue());
		if (loadFactor.floatValue() <= 0 || Float.isNaN(loadFactor.floatValue()))
			throw new IllegalArgumentException("Illegal Load factor: "+loadFactor.floatValue());
		if (initialCapacity.intValue()==0)
			initialCapacity = new Integer(1);
		setLoadFactor(loadFactor.floatValue());
		setTable(create(Entry.class,initialCapacity.intValue()));
		setThreshold((int)(initialCapacity.intValue() * getLoadFactor()));
	}

	public PersistentHashMap(Accessor accessor, Connection connection, Integer initialCapacity) throws RemoteException {
		this(accessor,connection,initialCapacity, new Float(0.75f));
	}

	public PersistentHashMap(Accessor accessor, Connection connection) throws RemoteException {
		this(accessor,connection,new Integer(11), new Float(0.75f));
	}

	public PersistentHashMap(Accessor accessor, Connection connection, RemoteMap t) throws RemoteException {
		this(accessor,connection,new Integer(Math.max(2*t.size(), 11)), new Float(0.75f));
		putAll(t);
	}

	public int size() {
	synchronized(mutex) {
		return getCount();
	}
	}

	public boolean isEmpty() {
	synchronized(mutex) {
		return getCount() == 0;
	}
	}

	public boolean containsValue(Object value) {
	synchronized(mutex) {
		Array tab = PersistentArrays.localArray(getTable());

		if (value==null) {
			for (int i = tab.length() ; i-- > 0 ;)
				for (Entry e = (Entry)tab.get(i) ; e != null ; e = e.getNext())
					if (e.getValue()==null)
						return true;
		} else {
			for (int i = tab.length() ; i-- > 0 ;)
				for (Entry e = (Entry)tab.get(i) ; e != null ; e = e.getNext())
					if (value.equals(e.getValue()))
						return true;
		}

		return false;
	}
	}

	public boolean containsKey(Object key) {
	synchronized(mutex) {
		Array tab = PersistentArrays.localArray(getTable());
		if (key != null) {
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % tab.length();
			for (Entry e = (Entry)tab.get(index); e != null; e = e.getNext())
				if (e.getHash()==hash && key.equals(e.getKey()))
					return true;
		} else {
			for (Entry e = (Entry)tab.get(0); e != null; e = e.getNext())
				if (e.getKey()==null)
					return true;
		}

		return false;
	}
	}

	public Object get(Object key) {
	synchronized(mutex) {
		Array tab = PersistentArrays.localArray(getTable());

		if (key != null) {
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % tab.length();
			for (Entry e = (Entry)tab.get(index); e != null; e = e.getNext())
				if ((e.getHash() == hash) && key.equals(e.getKey()))
					return e.getValue();
		} else {
			for (Entry e = (Entry)tab.get(0); e != null; e = e.getNext())
				if (e.getKey()==null)
					return e.getValue();
		}

		return null;
	}
	}

	private void rehash() {
		int oldCapacity = PersistentArrays.localArray(getTable()).length();
		Array oldMap = PersistentArrays.localArray(getTable());

		int newCapacity = oldCapacity * 2 + 1;
		Array newMap = PersistentArrays.localArray(create(Entry.class,newCapacity));

		setModCount(getModCount()+1);
		setThreshold((int)(newCapacity * getLoadFactor()));
		setTable(PersistentArrays.remoteArray(newMap));

		for (int i = oldCapacity ; i-- > 0 ;) {
			for (Entry old = (Entry)oldMap.get(i) ; old != null ; ) {
				Entry e = old;
				old = old.getNext();

				int index = (e.getHash() & 0x7FFFFFFF) % newCapacity;
				e.setNext((Entry)newMap.get(index));
				newMap.set(index,e);
			}
		}
	}

	public Object put(Object key, Object value) {
	synchronized(mutex) {
		Array tab = PersistentArrays.localArray(getTable());
		int hash = 0;
		int index = 0;

		if (key != null) {
			hash = key.hashCode();
			index = (hash & 0x7FFFFFFF) % tab.length();
			for (Entry e = (Entry)tab.get(index) ; e != null ; e = e.getNext()) {
				if ((e.getHash() == hash) && key.equals(e.getKey())) {
					Object old = e.getValue();
					e.setValue(value);
					return old;
				}
			}
		} else {
			for (Entry e = (Entry)tab.get(0); e != null ; e = e.getNext()) {
				if (e.getKey() == null) {
					Object old = e.getValue();
					e.setValue(value);
					return old;
				}
			}
		}

		setModCount(getModCount()+1);
		if (getCount() >= getThreshold()) {
			rehash();

			tab = PersistentArrays.localArray(getTable());
			index = (hash & 0x7FFFFFFF) % tab.length();
		}

		Entry e = (Entry)create(Entry.class, new Class[] {Integer.class, Object.class, Object.class, Entry.class}, new Object[] {new Integer(hash), key, value, tab.get(index)});
		tab.set(index,e);
		setCount(getCount()+1);
		return null;
	}
	}

	public Object remove(Object key) {
	synchronized(mutex) {
		Array tab = PersistentArrays.localArray(getTable());

		if (key != null) {
			int hash = key.hashCode();
			int index = (hash & 0x7FFFFFFF) % tab.length();

			for (Entry e = (Entry)tab.get(index), prev = null; e != null;
				 prev = e, e = e.getNext()) {
				if ((e.getHash() == hash) && key.equals(e.getKey())) {
					setModCount(getModCount()+1);
					if (prev != null)
						prev.setNext(e.getNext());
					else
						tab.set(index,e.getNext());

					setCount(getCount()-1);
					Object oldValue = e.getValue();
					e.setValue(null);
					return oldValue;
				}
			}
		} else {
			for (Entry e = (Entry)tab.get(0), prev = null; e != null;
				 prev = e, e = e.getNext()) {
				if (e.getKey() == null) {
					setModCount(getModCount()+1);
					if (prev != null)
						prev.setNext(e.getNext());
					else
						((Entry)tab.get(0)).setNext(e.getNext());

					setCount(getCount()-1);
					Object oldValue = e.getValue();
					e.setValue(null);
					return oldValue;
				}
			}
		}

		return null;
	}
	}

	public void putAll(RemoteMap t) {
	synchronized(mutex) {
		Iterator i = PersistentCollections.localMap(t).entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
			put(e.getKey(), e.getValue());
		}
	}
	}

	public void clear() {
	synchronized(mutex) {
		Array tab = PersistentArrays.localArray(getTable());
		setModCount(getModCount()+1);
		for (int index = tab.length(); --index >= 0; )
			tab.set(index,null);
		setCount(0);
	}
	}

	public RemoteSet keySet() throws RemoteException {
	synchronized(mutex) {
		return new KeySet(mutex);
	}
	}

	class KeySet extends TransientAbstractSet implements RemoteSet {
		KeySet(Object mutex) throws RemoteException {
			super(mutex);
		}
		public RemoteIterator iterator() throws RemoteException {
			return getHashIterator(KEYS);
		}
		public int size() {
		synchronized(mutex) {
			return getCount();
		}
		}
		public boolean contains(Object o) {
		synchronized(mutex) {
			return containsKey(o);
		}
		}
		public boolean remove(Object o) {
		synchronized(mutex) {
			int oldSize = getCount();
			PersistentHashMap.this.remove(o);
			return getCount() != oldSize;
		}
		}
		public void clear() {
		synchronized(mutex) {
			PersistentHashMap.this.clear();
		}
		}
	}

	public RemoteCollection values() throws RemoteException {
	synchronized(mutex) {
		return new Values(mutex);
	}
	}

	class Values extends TransientAbstractCollection implements RemoteCollection {
		Values(Object mutex) throws RemoteException {
			super(mutex);
		}
		public RemoteIterator iterator() throws RemoteException {
			return getHashIterator(VALUES);
		}
		public int size() {
		synchronized(mutex) {
			return getCount();
		}
		}
		public boolean contains(Object o) {
		synchronized(mutex) {
			return containsValue(o);
		}
		}
		public void clear() {
		synchronized(mutex) {
			PersistentHashMap.this.clear();
		}
		}
	}

	public RemoteSet entrySet() throws RemoteException {
	synchronized(mutex) {
		return new EntrySet(mutex);
	}
	}

	class EntrySet extends TransientAbstractSet implements RemoteSet {
		EntrySet(Object mutex) throws RemoteException {
			super(mutex);
		}
		public RemoteIterator iterator() throws RemoteException {
			return getHashIterator(ENTRIES);
		}

		public boolean contains(Object o) {
		synchronized(mutex) {
			if (!(o instanceof RemoteMap.Entry))
				return false;
			Map.Entry entry = PersistentCollections.localEntry((RemoteMap.Entry)o);
			Object key = entry.getKey();
			Array tab = PersistentArrays.localArray(getTable());
			int hash = (key==null ? 0 : key.hashCode());
			int index = (hash & 0x7FFFFFFF) % tab.length();
				for (Entry e = (Entry)tab.get(index); e != null; e = e.getNext())
				if (e.getHash()==hash && e.equals(entry))
					return true;
			return false;
		}
		}

		public boolean remove(Object o) {
		synchronized(mutex) {
			if (!(o instanceof RemoteMap.Entry))
				return false;
			Map.Entry entry = PersistentCollections.localEntry((RemoteMap.Entry)o);
			Object key = entry.getKey();
			Array tab = PersistentArrays.localArray(getTable());
			int hash = (key==null ? 0 : key.hashCode());
			int index = (hash & 0x7FFFFFFF) % tab.length();

			for (Entry e = (Entry)tab.get(index), prev = null; e != null;
				 prev = e, e = e.getNext()) {
				if (e.getHash()==hash && e.equals(entry)) {
					setModCount(getModCount()+1);
					if (prev != null)
						prev.setNext(e.getNext());
					else
						tab.set(index,e.getNext());

					setCount(getCount()-1);
					e.setValue(null);
					return true;
				}
			}
			return false;
		}
		}

		public int size() {
		synchronized(mutex) {
			return getCount();
		}
		}

		public void clear() {
		synchronized(mutex) {
			PersistentHashMap.this.clear();
		}
		}
	}

	private RemoteIterator getHashIterator(int type) throws RemoteException {
		if (getCount() == 0) {
			return emptyHashIterator;
		} else {
			return new HashIterator(type);
		}
	}

	public static class Entry extends PersistentObject implements RemoteMap.Entry {
		public int getHash() {
			return ((Integer)get("hash")).intValue();
		}

		public void setHash(int n) {
			set("hash",new Integer(n));
		}

		public Object getKey() {
			return get("key");
		}

		public void setKey(Object obj) {
			set("key",obj);
		}

		public Object getValue() {
			return get("value");
		}

		public Object setValue(Object obj) {
			Object oldValue = get("value");
			set("value",obj);
			return oldValue;
		}

		public Entry getNext() {
			return (Entry)get("next");
		}

		public void setNext(Entry entry) {
			set("next",entry);
		}

		public Entry() throws RemoteException {}

		public Entry(Accessor accessor, Connection connection, Integer hash, Object key, Object value, Entry next) throws RemoteException {
			super(accessor,connection);
			setHash(hash.intValue());
			setKey(key);
			setValue(value);
			setNext(next);
		}

		public Object clone() {
			return create(Entry.class, new Class[] {Integer.class, Object.class, Object.class, Entry.class}, new Object[] {new Integer(getHash()), getKey(), getValue(), (getNext()==null ? null : (Entry)getNext().clone())});
		}

//		public Object getKey() {
//			return key;
//		}

//		public Object getValue() {
//			return value;
//		}

//		public Object setValue(Object value) {
//			Object oldValue = this.value;
//			this.value = value;
//			return oldValue;
//		}

		public boolean equals(Object o) {
			if (!(o instanceof RemoteMap.Entry)) return false;
			Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry)o);

			return (getKey()==null ? e.getKey()==null : getKey().equals(e.getKey())) && (getValue()==null ? e.getValue()==null : getValue().equals(e.getValue()));
		}

		public int hashCode() {
			return getHash() ^ (getValue()==null ? 0 : getValue().hashCode());
		}

		public String remoteToString() {
			return getKey()+"="+getValue();
		}
	}

	private static final int KEYS = 0;
	private static final int VALUES = 1;
	private static final int ENTRIES = 2;

	private static EmptyHashIterator emptyHashIterator = new EmptyHashIterator();
											 
	private static class EmptyHashIterator implements RemoteIterator, Serializable {
		
		EmptyHashIterator() {
		}

		public boolean hasNext() {
			return false;
		}

		public Object next() {
			throw new NoSuchElementException();
		}
		
		public void remove() {
			throw new IllegalStateException();
		}

	}						
					
	private class HashIterator extends UnicastRemoteObject implements RemoteIterator {

		Array table = PersistentArrays.localArray(getTable());
		int index = table.length();
		Entry entry = null;
		Entry lastReturned = null;
		int type;

		private int expectedModCount = getModCount();

		HashIterator(int type) throws RemoteException {
			this.type = type;
		}

		public boolean hasNext() {
			Entry e = entry;
			int i = index;
			Array t = table;

			while (e == null && i > 0)
				e = (Entry)t.get(--i);
			entry = e;
			index = i;
			return e != null;
		}

		public Object next() {
			if (getModCount() != expectedModCount)
				throw new ConcurrentModificationException();

			Entry et = entry;
			int i = index;
			Array t = table;

			while (et == null && i > 0) 
				et = (Entry)t.get(--i);

			entry = et;
			index = i;
			if (et != null) {
				Entry e = lastReturned = entry;
				entry = e.getNext();
				return type == KEYS ? e.getKey() : (type == VALUES ? e.getValue() : e);
			}
			throw new NoSuchElementException();
		}

		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			if (getModCount() != expectedModCount)
				throw new ConcurrentModificationException();

			Array tab = PersistentArrays.localArray(getTable());
			int index = (lastReturned.getHash() & 0x7FFFFFFF) % tab.length();

			for (Entry e = (Entry)tab.get(index), prev = null; e != null;
				 prev = e, e = e.getNext()) {
				if (e == lastReturned) {
					setModCount(getModCount()+1);
					expectedModCount++;
					if (prev == null)
						tab.set(index,e.getNext());
					else
						prev.setNext(e.getNext());
					setCount(getCount()-1);
					lastReturned = null;
					return;
				}
			}
			throw new ConcurrentModificationException();
		}
	}

	int capacity() {
		return PersistentArrays.localArray(getTable()).length();
	}

	float loadFactor() {
		return getLoadFactor();
	}
}