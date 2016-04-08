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

public abstract class AbstractMap extends PersistentObject implements Map {
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
		Iterator i = entrySet().iterator();
		if (value==null) {
			while (i.hasNext()) {
				Entry e = (Entry) i.next();
				if (e.getValue()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry e = (Entry) i.next();
				if (value.equals(e.getValue()))
					return true;
			}
		}
		return false;
	}

	public boolean containsKey(Object key) {
		Iterator i = entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Entry e = (Entry) i.next();
				if (e.getKey()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry e = (Entry) i.next();
				if (key.equals(e.getKey()))
					return true;
			}
		}
		return false;
	}

	public synchronized Object get(Object key) {
		Iterator i = entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Entry e = (Entry) i.next();
				if (e.getKey()==null)
					return e.getValue();
			}
		} else {
			while (i.hasNext()) {
				Entry e = (Entry) i.next();
				if (key.equals(e.getKey()))
					return e.getValue();
			}
		}
		return null;
	}

	// Modification Operations

	abstract Object NULL();

	public synchronized Object put(Object key, Object value) {
		Object obj=put0(key,value);
		return obj==NULL()?null:obj;
	}

	Object put0(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	public synchronized Object remove(Object key) {
		Object obj=remove0(key);
		return obj==NULL()?null:obj;
	}

	Object remove0(Object key) {
		Iterator i = entrySet().iterator();
		Entry correctEntry = null;
		if (key==null) {
			while (correctEntry==null && i.hasNext()) {
				Entry e = (Entry) i.next();
				if (e.getKey()==null)
					correctEntry = e;
			}
		} else {
			while (correctEntry==null && i.hasNext()) {
				Entry e = (Entry) i.next();
				if (key.equals(e.getKey()))
					correctEntry = e;
			}
		}

		Object oldValue = NULL();
		if (correctEntry !=null) {
			oldValue = correctEntry.getValue();
			i.remove();
		}
		return oldValue;
	}

	// Bulk Operations

	public void putAll(Map t) {
		Iterator i = t.entrySet().iterator();
		while (i.hasNext()) {
			Entry e = (Entry) i.next();
			put(e.getKey(), e.getValue());
		}
	}

	public void clear() {
		entrySet().clear();
	}

	// Views

	transient volatile Set keySet = null;
	transient volatile Collection values = null;

	public Set keySet() {
		if (keySet == null) {
			keySet = new java.util.AbstractSet() {
				public Iterator iterator() {
					return new Iterator() {
						private Iterator i = entrySet().iterator();

						public boolean hasNext() {
							return i.hasNext();
						}

						public Object next() {
							return ((Entry)i.next()).getKey();
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

	public Collection values() {
		if (values == null) {
			values = new java.util.AbstractCollection() {
				public Iterator iterator() {
					return new Iterator() {
						private Iterator i = entrySet().iterator();

						public boolean hasNext() {
							return i.hasNext();
						}

						public Object next() {
							return ((Entry)i.next()).getValue();
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

	public abstract Set entrySet();

	// Comparison and hashing

	public synchronized boolean equals(PersistentObject o) {
		if (o == AbstractMap.this)
			return true;

		if (!(o instanceof Map))
			return false;
		Map t = (Map) o;
		if (t.size() != size())
			return false;

		try {
			Iterator i = entrySet().iterator();
			while (i.hasNext()) {
				Entry e = (Entry) i.next();
				Object key = e.getKey();
				Object value = e.getValue();
				if (value == null) {
					if (!(t.get(key)==null && t.containsKey(key)))
						return false;
				} else {
					if (!value.equals(t.get(key)))
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
		Iterator i = entrySet().iterator();
		while (i.hasNext())
			h += i.next().hashCode();
		return h;
	}

	public synchronized String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("{");

		Iterator i = entrySet().iterator();
		boolean hasNext = i.hasNext();
		while (hasNext) {
			Entry e = (Entry) (i.next());
			Object key = e.getKey();
			Object value = e.getValue();
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
