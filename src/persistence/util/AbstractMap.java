/*
 * @(#)AbstractMap.java		1.34 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import persistence.PersistentObject;
import persistence.Store;

public abstract class AbstractMap<K,V> extends PersistentObject implements Map<K,V> {
	public AbstractMap() {
	}

	public AbstractMap(final Store store) {
		super(store);
	}

	protected String[] secondary() {
		return concat(super.secondary(), new String[] {"empty"});
	}

	// Query Operations

	public int size() {
		return entrySet().size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean containsValue(Object value) {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		if (value==null) {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getValue()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (value.equals(e.getValue()))
					return true;
			}
		}
		return false;
	}

	public boolean containsKey(Object key) {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getKey()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (key.equals(e.getKey()))
					return true;
			}
		}
		return false;
	}

	public synchronized V get(Object key) {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getKey()==null)
					return e.getValue();
			}
		} else {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (key.equals(e.getKey()))
					return e.getValue();
			}
		}
		return null;
	}

	// Modification Operations

	abstract V NULL();

	public synchronized V put(K key, V value) {
		V obj=put0(key,value);
		return obj==NULL()?null:obj;
	}

	V put0(K key, V value) {
		throw new UnsupportedOperationException();
	}

	public synchronized V remove(Object key) {
		V obj=remove0(key);
		return obj==NULL()?null:obj;
	}

	V remove0(Object key) {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		Entry<K,V> correctEntry = null;
		if (key==null) {
			while (correctEntry==null && i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getKey()==null)
					correctEntry = e;
			}
		} else {
			while (correctEntry==null && i.hasNext()) {
				Entry<K,V> e = i.next();
				if (key.equals(e.getKey()))
					correctEntry = e;
			}
		}

		V oldValue = NULL();
		if (correctEntry !=null) {
			oldValue = correctEntry.getValue();
			i.remove();
		}
		return oldValue;
	}

	// Bulk Operations

	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	public void clear() {
		entrySet().clear();
	}

	// Views

	transient volatile Set<K> keySet = null;
	transient volatile Collection<V> values = null;

	public Set<K> keySet() {
		if (keySet == null) {
			keySet = new java.util.AbstractSet<K>() {
				public Iterator<K> iterator() {
					return new Iterator<K>() {
						private Iterator<Entry<K,V>> i = entrySet().iterator();

						public boolean hasNext() {
							return i.hasNext();
						}

						public K next() {
							return i.next().getKey();
						}

						public void remove() {
							i.remove();
						}
					};
				}

				public int size() {
					return AbstractMap.this.size();
				}

				public boolean contains(Object k) {
					return AbstractMap.this.containsKey(k);
				}
			};
		}
		return keySet;
	}

	public Collection<V> values() {
		if (values == null) {
			values = new java.util.AbstractCollection<V>() {
				public Iterator<V> iterator() {
					return new Iterator<V>() {
						private Iterator<Entry<K,V>> i = entrySet().iterator();

						public boolean hasNext() {
							return i.hasNext();
						}

						public V next() {
							return i.next().getValue();
						}

						public void remove() {
							i.remove();
						}
					};
				}

				public int size() {
					return AbstractMap.this.size();
				}

				public boolean contains(Object v) {
					return AbstractMap.this.containsValue(v);
				}
			};
		}
		return values;
	}

	public abstract Set<Entry<K,V>> entrySet();

	// Comparison and hashing

	@SuppressWarnings("unchecked")
	public synchronized boolean equals(PersistentObject o) {
		if (o == AbstractMap.this)
			return true;

		if (!(o instanceof Map))
			return false;
		Map<K,V> m = (Map<K,V>) o;
		if (m.size() != size())
			return false;

		try {
			Iterator<Entry<K,V>> i = entrySet().iterator();
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				K key = e.getKey();
				V value = e.getValue();
				if (value == null) {
					if (!(m.get(key)==null && m.containsKey(key)))
						return false;
				} else {
					if (!value.equals(m.get(key)))
						return false;
				}
			}
		} catch(ClassCastException unused)   {
			return false;
		} catch(NullPointerException unused) {
			return false;
		}

		return true;
	}

	public synchronized int hashCode() {
		int h = 0;
		Iterator<Entry<K,V>> i = entrySet().iterator();
		while (i.hasNext())
			h += i.next().hashCode();
		return h;
	}

	public synchronized String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("{");

		Iterator<Entry<K,V>> i = entrySet().iterator();
		boolean hasNext = i.hasNext();
		while (hasNext) {
			Entry<K,V> e = i.next();
			K key = e.getKey();
			V value = e.getValue();
			buf.append((key == AbstractMap.this ?  "(this Map)" : key) + "=" +
				(value == AbstractMap.this ? "(this Map)": value));

			hasNext = i.hasNext();
			if (hasNext)
				buf.append(", ");
		}

		buf.append("}");
		return buf.toString();
	}
}
