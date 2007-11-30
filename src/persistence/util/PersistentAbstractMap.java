/*
 * @(#)AbstractMap.java	1.34 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Map;
import persistence.Accessor;
import persistence.Connection;
import persistence.PersistentObject;

public abstract class PersistentAbstractMap extends PersistentObject implements RemoteMap {
	public PersistentAbstractMap() throws RemoteException {}

	public PersistentAbstractMap(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
	}

	public int size() {
	synchronized(mutex()) {
		return PersistentCollections.localMap(this).entrySet().size();
	}
	}

	public boolean isEmpty() {
	synchronized(mutex()) {
		return size() == 0;
	}
	}

	public boolean containsValue(Object value) {
		return ((Boolean)execute(
			methodCall("containsValue",new Class[] {Object.class},new Object[] {value}))).booleanValue();
	}

	public Boolean containsValueImpl(Object value) {
		return new Boolean(containsValue0(value));
	}

	boolean containsValue0(Object value) {
	synchronized(mutex()) {
		Iterator i = PersistentCollections.localMap(this).entrySet().iterator();
		if (value==null) {
			while (i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
				if (e.getValue()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
				if (value.equals(e.getValue()))
					return true;
			}
		}
		return false;
	}
	}

	public boolean containsKey(Object key) {
		return ((Boolean)execute(
			methodCall("containsKey",new Class[] {Object.class},new Object[] {key}))).booleanValue();
	}

	public Boolean containsKeyImpl(Object key) {
		return new Boolean(containsKey0(key));
	}

	boolean containsKey0(Object key) {
	synchronized(mutex()) {
		Iterator i = PersistentCollections.localMap(this).entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
				if (e.getKey()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
				if (key.equals(e.getKey()))
					return true;
			}
		}
		return false;
	}
	}

	public Object get(Object key) {
		return execute(
			methodCall("get",new Class[] {Object.class},new Object[] {key}));
	}

	public Object getImpl(Object key) {
	synchronized(mutex()) {
		Iterator i = PersistentCollections.localMap(this).entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
				if (e.getKey()==null)
					return e.getValue();
			}
		} else {
			while (i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
				if (key.equals(e.getKey()))
					return e.getValue();
			}
		}
		return null;
	}
	}

	public Object put(Object key, Object value) {
		Object obj=execute(
			methodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,value}),
			methodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
		return obj==NULL?null:obj;
	}

	public Object remove(Object key) {
		Object obj=execute(
			methodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,NULL}),
			methodCall("put",new Class[] {Object.class,Object.class},new Object[] {key,null}),1);
		return obj==NULL?null:obj;
	}

	static Object NULL=new Object();

	public Object putImpl(Object key, Object value) {
		return value==NULL?remove0(key):put0(key,value);
	}

	Object put0(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	Object remove0(Object key) {
	synchronized(mutex()) {
		Iterator i = PersistentCollections.localMap(this).entrySet().iterator();
		Map.Entry correctEntry = null;
		if (key==null) {
			while (correctEntry==null && i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
				if (e.getKey()==null)
					correctEntry = e;
			}
		} else {
			while (correctEntry==null && i.hasNext()) {
				Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
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
	}

	public void putAll(RemoteMap t) {
	synchronized(mutex()) {
		Iterator i = PersistentCollections.localMap(t).entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
			put(e.getKey(), e.getValue());
		}
	}
	}

	public void clear() {
	synchronized(mutex()) {
		PersistentCollections.localMap(this).entrySet().clear();
	}
	}

	public RemoteSet keySet() throws RemoteException {
	synchronized(mutex()) {
		return new KeySet(mutex());
	}
	}

	class KeySet extends TransientAbstractSet implements RemoteSet {
		KeySet(Object mutex) throws RemoteException {
			super(mutex);
		}

		class Itr extends UnicastRemoteObject implements RemoteIterator {
			private Iterator i;

			Itr(Iterator i) throws RemoteException {
				this.i = i;
			}

			public boolean hasNext() {
				return i.hasNext();
			}

			public Object next() {
				return PersistentCollections.localEntry((RemoteMap.Entry)i.next()).getKey();
			}

			public void remove() {
				i.remove();
			}
		}

		public RemoteIterator iterator() throws RemoteException {
			return new Itr(PersistentCollections.localMap(PersistentAbstractMap.this).entrySet().iterator());
		}

		public int size() {
		synchronized(mutex()) {
			return PersistentAbstractMap.this.size();
		}
		}

		public boolean contains(Object k) {
		synchronized(mutex()) {
			return PersistentAbstractMap.this.containsKey(k);
		}
		}
	}

	public RemoteCollection values() throws RemoteException {
	synchronized(mutex()) {
		return new Values(mutex());
	}
	}

	class Values extends TransientAbstractCollection implements RemoteCollection {
		Values(Object mutex) throws RemoteException {
			super(mutex);
		}

		class Itr extends UnicastRemoteObject implements RemoteIterator {
			private Iterator i;

			Itr(Iterator i) throws RemoteException {
				this.i = i;
			}

			public boolean hasNext() {
				return i.hasNext();
			}

			public Object next() {
				return PersistentCollections.localEntry((RemoteMap.Entry)i.next()).getKey();
			}

			public void remove() {
				i.remove();
			}
		}

		public RemoteIterator iterator() throws RemoteException {
			return new Itr(PersistentCollections.localMap(PersistentAbstractMap.this).entrySet().iterator());
		}

		public int size() {
		synchronized(mutex()) {
			return PersistentAbstractMap.this.size();
		}
		}

		public boolean contains(Object v) {
		synchronized(mutex()) {
			return PersistentAbstractMap.this.containsValue(v);
		}
		}
	}

	public abstract RemoteSet entrySet() throws RemoteException;

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof RemoteMap))
			return false;
		Map t = PersistentCollections.localMap((RemoteMap) o);
		if (t.size() != size())
			return false;

		Iterator i = PersistentCollections.localMap(this).entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) i.next());
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
		return true;
	}

	public int hashCode() {
		int h = 0;
		Iterator i = PersistentCollections.localMap(this).entrySet().iterator();
		while (i.hasNext())
			h += i.next().hashCode();
		return h;
	}

	public String remoteToString() {
	synchronized(mutex()) {
		int max = size() - 1;
		StringBuffer buf = new StringBuffer();
		Iterator i = PersistentCollections.localMap(this).entrySet().iterator();

		buf.append("{");
		for (int j = 0; j <= max; j++) {
			Map.Entry e = PersistentCollections.localEntry((RemoteMap.Entry) (i.next()));
			buf.append(e.getKey() + "=" + e.getValue());
			if (j < max)
				buf.append(", ");
		}
		buf.append("}");
		return buf.toString();
	}
	}
}
