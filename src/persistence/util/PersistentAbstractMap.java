/*
 * @(#)AbstractMap.java		1.34 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import persistence.MethodCall;
import persistence.PersistentObject;

public abstract class PersistentAbstractMap extends PersistentObject implements Map {
	protected Accessor createAccessor() {
		return new Accessor() {
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

			public Object put(Object key, Object value) {
				return value==NULL?remove0(key):put0(key,value);
			}

			public Object put0(Object key, Object value) {
				throw new UnsupportedOperationException();
			}

			public synchronized Object remove0(Object key) {
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

				Object oldValue = null;
				if (correctEntry !=null) {
					oldValue = correctEntry.getValue();
					i.remove();
				}
				return oldValue;
			}
		};
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

	public Object get(Object key) {
		return execute(
			new MethodCall(this,"get",new Class[] {Object.class},new Object[] {key}));
	}

	// Modification Operations

	public Object put(Object key, Object value) {
		Object obj=execute(
			new MethodCall(this,"put",new Class[] {Object.class,Object.class},new Object[] {key,value}),
			new MethodCall(this,"put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
		return obj==NULL?null:obj;
	}

	public Object remove(Object key) {
		Object obj=execute(
			new MethodCall(this,"put",new Class[] {Object.class,Object.class},new Object[] {key,NULL}),
			new MethodCall(this,"put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
		return obj==NULL?null:obj;
	}

	static final Object NULL=new Object();

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

	public Set keySet() {
		return new AbstractSet() {
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
				return PersistentAbstractMap.this.size();
			}

			public boolean contains(Object k) {
				return PersistentAbstractMap.this.containsKey(k);
			}
		};
	}

	public Collection values() {
		return new AbstractCollection() {
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
				return PersistentAbstractMap.this.size();
			}

			public boolean contains(Object v) {
				return PersistentAbstractMap.this.containsValue(v);
			}
		};
	}

	public abstract Set entrySet();

	// Comparison and hashing

	public boolean equals(Object o) {
		if (o == this)
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

	public int hashCode() {
		int h = 0;
		Iterator i = entrySet().iterator();
		while (i.hasNext())
			h += i.next().hashCode();
		return h;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("{");

		Iterator i = entrySet().iterator();
		boolean hasNext = i.hasNext();
		while (hasNext) {
			Entry e = (Entry) (i.next());
			Object key = e.getKey();
			Object value = e.getValue();
			buf.append((key == this ?  "(this Map)" : key) + "=" + 
					   (value == this ? "(this Map)": value));

			hasNext = i.hasNext();
			if (hasNext)
				buf.append(", ");
		}

		buf.append("}");
		return buf.toString();
	}

	static class SimpleEntry implements Entry {
		Object key;
		Object value;

		public SimpleEntry(Object key, Object value) {
			this.key   = key;
			this.value = value;
		}

		public SimpleEntry(Map.Entry e) {
			this.key   = e.getKey();
			this.value = e.getValue();
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}

		public Object setValue(Object value) {
			Object oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry)o;
			return eq(key, e.getKey()) &&  eq(value, e.getValue());
		}

		public int hashCode() {
			Object v;
			return ((key   == null)   ? 0 :   key.hashCode()) ^
				   ((value == null)   ? 0 : value.hashCode());
		}

		public String toString() {
			return key + "=" + value;
		}

		private static boolean eq(Object o1, Object o2) {
			return (o1 == null ? o2 == null : o1.equals(o2));
		}
	}
}
