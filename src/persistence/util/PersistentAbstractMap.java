/*
 * @(#)AbstractMap.java		1.34 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import persistence.PersistentObject;

public abstract class PersistentAbstractMap extends PersistentObject implements Map {
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		public Object get(Object key) {
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

		public Object remove0(Object key) {
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
			
			Object oldValue = NULL;
			if (correctEntry !=null) {
				oldValue = correctEntry.getValue();
				i.remove();
			}
			return oldValue;
		}

		// Comparison and hashing

		public boolean remoteEquals(PersistentObject o) {
			if (o == PersistentAbstractMap.this)
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

		public int remoteHashCode() {
			int h = 0;
			Iterator i = entrySet().iterator();
			while (i.hasNext())
				h += i.next().hashCode();
			return h;
		}

		public String remoteToString() {
			StringBuffer buf = new StringBuffer();
			buf.append("{");

			Iterator i = entrySet().iterator();
			boolean hasNext = i.hasNext();
			while (hasNext) {
				Entry e = (Entry) (i.next());
				Object key = e.getKey();
				Object value = e.getValue();
				buf.append((key == PersistentAbstractMap.this ?  "(this Map)" : key) + "=" +
					(value == PersistentAbstractMap.this ? "(this Map)": value));

				hasNext = i.hasNext();
				if (hasNext)
					buf.append(", ");
			}

			buf.append("}");
			return buf.toString();
		}
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
			new MethodCall("get",new Class[] {Object.class},new Object[] {key}));
	}

	// Modification Operations

	public Object put(Object key, Object value) {
		Object obj=execute(
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,value}),
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
		return obj==NULL?null:obj;
	}

	public Object remove(Object key) {
		Object obj=execute(
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,NULL}),
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
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
}
