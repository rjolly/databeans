/*
 * @(#)AbstractMap.java		1.34 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import persistence.PersistentClass;
import persistence.PersistentObject;

public abstract class AbstractMap extends PersistentObject implements Map {
	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

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

		public synchronized Object put(Object key, Object value) {
			return value==((AbstractMapClass)persistentClass()).NULL()?remove0(key):put0(key,value);
		}

		Object put0(Object key, Object value) {
			throw new UnsupportedOperationException();
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
			
			Object oldValue = ((AbstractMapClass)persistentClass()).NULL();
			if (correctEntry !=null) {
				oldValue = correctEntry.getValue();
				i.remove();
			}
			return oldValue;
		}

		// Comparison and hashing

		public synchronized boolean persistentEquals(PersistentObject o) {
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

		public synchronized int persistentHashCode() {
			int h = 0;
			Iterator i = entrySet().iterator();
			while (i.hasNext())
				h += i.next().hashCode();
			return h;
		}

		public synchronized String persistentToString() {
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

	protected PersistentClass createClass() {
		return (PersistentClass)create(AbstractMapClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	// Query Operations

	public int size() {
		return entrySet().size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean _containsValue(Object value) {
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

	public boolean containsValue(Object value) {
		return ((Boolean)execute(
			new MethodCall("_containsValue",new Class[] {Object.class},new Object[] {value}))).booleanValue();
	}

	public boolean _containsKey(Object key) {
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

	public boolean containsKey(Object key) {
		return ((Boolean)execute(
			new MethodCall("_containsKey",new Class[] {Object.class},new Object[] {key}))).booleanValue();
	}

	public Object get(Object key) {
		return executeAtomic(
			new MethodCall("get",new Class[] {Object.class},new Object[] {key}));
	}

	// Modification Operations

	public Object put(Object key, Object value) {
		Object obj=executeAtomic(
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,value}),
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
		return obj==((AbstractMapClass)persistentClass()).NULL()?null:obj;
	}

	public Object remove(Object key) {
		Object obj=executeAtomic(
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,((AbstractMapClass)persistentClass()).NULL()}),
			new MethodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
		return obj==((AbstractMapClass)persistentClass()).NULL()?null:obj;
	}

	// Bulk Operations

	public void _putAll(Map t) {
		Iterator i = t.entrySet().iterator();
		while (i.hasNext()) {
			Entry e = (Entry) i.next();
			put(e.getKey(), e.getValue());
		}
	}

	public void putAll(Map t) {
		execute(
			new MethodCall("_putAll",new Class[] {Map.class},new Object[] {t}));
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
}
