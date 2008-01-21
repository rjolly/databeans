/*
 * @(#)HashMap.java		1.59 04/12/09
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import persistence.Array;
import persistence.PersistentClass;
import persistence.PersistentObject;

public class PersistentHashMap extends PersistentAbstractMap implements Map, Cloneable
{
	static final int DEFAULT_INITIAL_CAPACITY = 16;
	static final int MAXIMUM_CAPACITY = 1 << 30;
	static final float DEFAULT_LOAD_FACTOR = 0.75f;

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentAbstractMap.Accessor {
		public Accessor() throws RemoteException {}

		public void init(int initialCapacity, float loadFactor) {
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
			setTable(create(Entry.class,capacity));
			init0();
		}

		public void init() {
			setLoadFactor(DEFAULT_LOAD_FACTOR);
			setThreshold((int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR));
			setTable(create(Entry.class,DEFAULT_INITIAL_CAPACITY));
			init0();
		}

		public void init(Map m) {
			init(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
				DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
			putAllForCreate(m);
		}
 
		public int size() {
			return getSize();
		}

		public synchronized boolean containsKey(Object key) {
			Object k = maskNull(key);
			int hash = hash(k);
			int i = indexFor(hash, getTable().length());
			Entry e = (Entry)getTable().get(i);
			while (e != null) {
				if (e.getHash() == hash && eq(k, e.getKey()))
					return true;
				e=e.getNext();
			}
			return false;
		}

		public synchronized Entry getEntry(Object key) {
			Object k = maskNull(key);
			int hash = hash(k);
			int i = indexFor(hash, getTable().length());
			Entry e = (Entry)getTable().get(i);
			while (e != null && !(e.getHash() == hash && eq(k, e.getKey0())))
				e=e.getNext();
			return e;
		}

		public synchronized boolean containsValue(Object value) {
			if (value == null)
				return containsNullValue();
			
			Array tab = getTable();
			for (int i = 0; i < tab.length() ; i++)
				for (Entry e = (Entry)tab.get(i) ; e != null ; e = e.getNext())
					if (value.equals(e.getValue()))
						return true;
			return false;
		}

		public synchronized Entry nextEntry(Entry entry) {
			Entry n;
			Array t = getTable();
			if(entry == null) {
				n = entry;
				int i = t.length();
				if (getSize() != 0) {
					while (n == null && i > 0) n = (Entry)t.get(--i);
				}
			} else {
				n = entry.getNext();
				Object k = maskNull(entry.getKey0());
				int hash = hash(k);
				int i = indexFor(hash, t.length());
				while (n == null && i > 0) n = (Entry)t.get(--i);
			}
			return n;
		}

		public Map.Entry putMapping(Map.Entry entry) {
			return putMapping(entry.getKey(),entry.getValue());
		}

		public synchronized Map.Entry putMapping(Object key, Object value) {
			Object k = maskNull(key);
			int hash = hash(k);
			int i = indexFor(hash, getTable().length());
			
			setModCount(getModCount()+1);
			return addEntry(hash, k, value, i);
		}

		public synchronized Map.Entry removeMapping(Map.Entry entry) {
			Object k = maskNull(entry.getKey());
			int hash = hash(k);
			int i = indexFor(hash, getTable().length());
			Entry prev = (Entry)getTable().get(i);
			Entry e = prev;
			
			while (e != null) {
				Entry next = e.getNext();
				if (e.getHash() == hash && e.equals(entry)) {
					setModCount(getModCount()+1);
					setSize(getSize()-1);
					if (prev == e)
						getTable().set(i,next);
					else
						prev.setNext(next);
					e.recordRemoval(PersistentHashMap.this);
					return e;
				}
				prev = e;
				e = next;
			}
			
			return e;
		}

		public synchronized PersistentObject persistentClone() {
			PersistentHashMap result = (PersistentHashMap)super.persistentClone();
			result.setTable(create(Entry.class,getTable().length()));
			result.setModCount(0);
			result.setSize(0);
			result.init0();
			result.putAllForCreate(PersistentHashMap.this);
			
			return result;
		}

		public int modCount() {
			return getModCount();
		}
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(HashMapClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	public Array getTable() {
		return (Array)get("table");
	}

	public void setTable(Array array) {
		set("table",array);
	}

	public int getSize() {
		return ((Integer)get("size")).intValue();
	}

	public void setSize(int n) {
		set("size",new Integer(n));
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

	public void init(int initialCapacity, float loadFactor) {
		execute(
			new MethodCall("init",new Class[] {int.class,float.class},new Object[] {new Integer(initialCapacity),new Float(loadFactor)}));
	}

	public void init(int initialCapacity) {
		init(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public void init() {
		execute(
			new MethodCall("init",new Class[] {},new Object[] {}));
	}

	public void init(Map m) {
		execute(
			new MethodCall("init",new Class[] {Map.class},new Object[] {m}));
	}

	void init0() {}

	// internal utilities

	Object maskNull(Object key) {
		return (key == null ? ((HashMapClass)persistentClass()).NULL_KEY() : key);
	}

	Object unmaskNull(Object key) {
		return (key == ((HashMapClass)persistentClass()).NULL_KEY() ? null : key);
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
		return ((Integer)execute(
			new MethodCall("size",new Class[] {},new Object[] {}))).intValue();
	}
  
//	public boolean isEmpty() {
//		return getSize() == 0;
//	}

	public Object get(Object key) {
		Entry e=getEntry(key);
		return e==null?e:e.getValue();
	}

	public boolean containsKey(Object key) {
		return ((Boolean)execute(
			new MethodCall("containsKey",new Class[] {Object.class},new Object[] {key}))).booleanValue();
	}

	Entry getEntry(Object key) {
		return (Entry)execute(
			new MethodCall("getEntry",new Class[] {Object.class},new Object[] {key}));
	}

	public Object put(Object key, Object value) {
		Entry e = getEntry(key);
		if(e != null) {
			Object oldValue = e.getValue();
			e.setValue(value);
			e.recordAccess(this);
			return oldValue;
		} else return putMapping(key,value);
	}

	private void putForCreate(Object key, Object value) {
		Object k = maskNull(key);
		int hash = hash(k);
		int i = indexFor(hash, getTable().length());

		for (Entry e = (Entry)getTable().get(i); e != null; e = e.getNext()) {
			if (e.getHash() == hash && eq(k, e.getKey())) {
				e.setValue(value);
				return;
			}
		}

		createEntry(hash, k, value, i);
	}

	void putAllForCreate(Map m) {
		for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry) i.next();
			putForCreate(e.getKey(), e.getValue());
		}
	}

	void resize(int newCapacity) {
		Array oldTable = getTable();
		int oldCapacity = oldTable.length();
		if (oldCapacity == MAXIMUM_CAPACITY) {
			setThreshold(Integer.MAX_VALUE);
			return;
		}

		Array newTable = create(Entry.class,newCapacity);
		transfer(newTable);
		setTable(newTable);
		setThreshold((int)(newCapacity * getLoadFactor()));
	}

	void transfer(Array newTable) {
		Array src = getTable();
		int newCapacity = newTable.length();
		for (int j = 0; j < src.length(); j++) {
			Entry e = (Entry)src.get(j);
			if (e != null) {
				src.set(j,null);
				do {
					Entry next = e.getNext();
					int i = indexFor(e.getHash(), newCapacity);  
					e.setNext((Entry)newTable.get(i));
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
//		for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
//			Map.Entry e = (Map.Entry) i.next();
//			put(e.getKey(), e.getValue());
//		}
//	}
  
	public Object remove(Object key) {
		return(removeEntryForKey(key));
	}

	Entry removeEntryForKey(Object key) {
		return (Entry)removeMapping(getEntry(key));
	}

	Map.Entry putMapping(Object key, Object value) {
		return (Map.Entry)execute(
			new MethodCall("putMapping",new Class[] {Object.class,Object.class},new Object[] {key,value}),
			new MethodCall("removeMapping",new Class[] {Map.Entry.class},new Object[] {null}),0);
	}

	Map.Entry removeMapping(Map.Entry entry) {
		return (Map.Entry)execute(
			new MethodCall("removeMapping",new Class[] {Map.Entry.class},new Object[] {entry}),
			new MethodCall("putMapping",new Class[] {Map.Entry.class},new Object[] {null}),0);
	}

//	public void clear() {
//		setModCount(getModCount()+1);
//		Array tab = getTable();
//		for (int i = 0; i < tab.length(); i++) 
//			tab.set(i,null);
//		setSize(0);
//	}

	public boolean containsValue(Object value) {
		return ((Boolean)execute(
			new MethodCall("containsValue",new Class[] {Object.class},new Object[] {value}))).booleanValue();
	}

	private boolean containsNullValue() {
		Array tab = getTable();
		for (int i = 0; i < tab.length() ; i++)
			for (Entry e = (Entry)tab.get(i) ; e != null ; e = e.getNext())
				if (e.getValue() == null)
					return true;
		return false;
	}

	public static class Entry extends PersistentObject implements Map.Entry {
		protected PersistentObject.Accessor createAccessor() throws RemoteException {
			return new Accessor();
		}

		protected class Accessor extends PersistentObject.Accessor {
			public Accessor() throws RemoteException {}

			public void init(int h, Object k, Object v, Entry n) {
				setValue0(v);
				setNext(n);
				setKey0(k);
				setHash(h);
			}

			Object unmaskNull(Object key) {
				return (key == ((HashMapClass)Entry.this.get(PersistentHashMap.class)).NULL_KEY() ? null : key);
			}

			public Object getKey() {
				return unmaskNull(getKey0());
			}

			public Object getValue() {
				return getValue0();
			}
	
			public Object setValue(Object newValue) {
				Object oldValue = getValue0();
				setValue0(newValue);
				return oldValue;
			}
	
			public boolean persistentEquals(Object o) {
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
	
			public int persistentHashCode() {
				return (getKey0()==((HashMapClass)Entry.this.get(PersistentHashMap.class)).NULL_KEY() ? 0 : getKey0().hashCode()) ^
					(getValue0()==null  ? 0 : getValue0().hashCode());
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

		public int getHash() {
			return ((Integer)get("hash")).intValue();
		}

		public void setHash(int n) {
			set("hash",new Integer(n));
		}

		public Entry getNext() {
			return (Entry)get("next");
		}

		public void setNext(Entry entry) {
			set("next",entry);
		}

		public void init(int h, Object k, Object v, Entry n) { 
			execute(
				new MethodCall("init",new Class[] {int.class,Object.class,Object.class,Entry.class},new Object[] {new Integer(h), k, v, n}));
		}

		public Object getKey() {
			return execute(
				new MethodCall("getKey",new Class[] {},new Object[] {}));
		}

		public Object getValue() {
			return execute(
				new MethodCall("getValue",new Class[] {},new Object[] {}));
		}
	
		public Object setValue(Object newValue) {
			return execute(
				new MethodCall("setValue",new Class[] {Object.class},new Object[] {newValue}),
				new MethodCall("setValue",new Class[] {Object.class},new Object[] {null}),0);
		}

		void recordAccess(PersistentHashMap m) {}

		void recordRemoval(PersistentHashMap m) {}
	}

	Entry addEntry(int hash, Object key, Object value, int bucketIndex) {
		Entry entry=createEntry(hash,key,value,bucketIndex);
		if (getSize() >= getThreshold()) 
			resize(2 * getTable().length());
		return entry;
	}

	Entry createEntry(int hash, Object key, Object value, int bucketIndex) {
		Entry entry;
		getTable().set(bucketIndex,entry=(Entry)create(Entry.class,new Class[] {int.class,Object.class,Object.class,Entry.class},new Object[] {new Integer(hash), key, value, getTable().get(bucketIndex)}));
		setSize(getSize()+1);
		return entry;
	}

	Entry nextEntry(Entry entry) {
		return (Entry)execute(
			new MethodCall("nextEntry",new Class[] {Entry.class},new Object[] {entry}));
	}

	int modCount() {
		return ((Integer)execute(
			new MethodCall("modCount",new Class[] {},new Object[] {}))).intValue();
	}

	private abstract class HashIterator implements Iterator {
		Entry next;				  // next entry to return
		int expectedModCount;		// For fast-fail 
		Entry current;			   // current entry

		HashIterator() {
			expectedModCount = modCount();
			next = PersistentHashMap.this.nextEntry(null);
		}

		public boolean hasNext() {
			return next != null;
		}

		Entry nextEntry() { 
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			Entry e = next;
			if (e == null) 
				throw new NoSuchElementException();

			next = PersistentHashMap.this.nextEntry(e);
			return current = e;
		}

		public void remove() {
			if (current == null)
				throw new IllegalStateException();
			if (modCount() != expectedModCount)
				throw new ConcurrentModificationException();
			Object k = current.getKey();
			current = null;
			PersistentHashMap.this.removeEntryForKey(k);
			expectedModCount = modCount();
		}

	}

	private class ValueIterator extends HashIterator {
		public Object next() {
			return nextEntry().getValue();
		}
	}

	private class KeyIterator extends HashIterator {
		public Object next() {
			return nextEntry().getKey();
		}
	}

	private class EntryIterator extends HashIterator {
		public Object next() {
			return nextEntry();
		}
	}

	// Subclass overrides these to alter behavior of views' iterator() method
	Iterator newKeyIterator()   {
		return new KeyIterator();
	}
	Iterator newValueIterator()   {
		return new ValueIterator();
	}
	Iterator newEntryIterator()   {
		return new EntryIterator();
	}

	// Views

	private transient Set entrySet = null;

	public Set keySet() {
		Set ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet()));
	}

	private class KeySet extends AbstractSet {
		public Iterator iterator() {
			return newKeyIterator();
		}
		public int size() {
			return PersistentHashMap.this.size();
		}
		public boolean contains(Object o) {
			return containsKey(o);
		}
		public boolean remove(Object o) {
			return PersistentHashMap.this.removeEntryForKey(o) != null;
		}
//		public void clear() {
//			PersistentHashMap.this.clear();
//		}
	}

	public Collection values() {
		Collection vs = values;
		return (vs != null ? vs : (values = new Values()));
	}

	private class Values extends AbstractCollection {
		public Iterator iterator() {
			return newValueIterator();
		}
		public int size() {
			return PersistentHashMap.this.size();
		}
		public boolean contains(Object o) {
			return containsValue(o);
		}
//		public void clear() {
//			PersistentHashMap.this.clear();
//		}
	}

	public Set entrySet() {
		Set es = entrySet;
		return (es != null ? es : (entrySet = new EntrySet()));
	}

	private class EntrySet extends AbstractSet {
		public Iterator iterator() {
			return newEntryIterator();
		}
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry)o;
			Entry candidate = getEntry(e.getKey());
			return candidate != null && candidate.equals(e);
		}
		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			
			Map.Entry entry = (Map.Entry)o;
			return removeMapping(entry) != null;
		}
		public int size() {
			return PersistentHashMap.this.size();
		}
//		public void clear() {
//			PersistentHashMap.this.clear();
//		}
	}
}
