/*
 * @(#)AbstractList.java	1.37 03/01/18
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import persistence.Accessor;
import persistence.Connection;

public abstract class PersistentAbstractList extends PersistentAbstractCollection implements RemoteList {
	public PersistentAbstractList() throws RemoteException {}

	public PersistentAbstractList(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
	}

	public boolean add(Object o) {
	synchronized(mutex()) {
		add(size(), o);
		return true;
	}
	}

	public Object get(int index) {
		return execute(
			methodCall("get",new Class[] {Integer.class},new Object[] {new Integer(index)}));
	}

	public Object getImpl(Integer index) {
		return get0(index.intValue());
	}

	abstract Object get0(int index);

	public Object set(int index, Object element) {
		return execute(
			methodCall("set",new Class[] {Integer.class,Object.class},new Object[] {new Integer(index),element}),
			methodCall("set",new Class[] {Integer.class,Object.class},new Object[] {new Integer(index),null}),1);
	}

	public Object setImpl(Integer index, Object element) {
		return set0(index.intValue(),element);
	}

	Object set0(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	public void add(int index, Object element) {
		execute(
			methodCall("add",new Class[] {Integer.class,Object.class},new Object[] {new Integer(index),element}),
			methodCall("remove",new Class[] {Integer.class},new Object[] {null}),0);
	}

	public Integer addImpl(Integer index, Object element) {
		return new Integer(add0(index.intValue(),element));
	}

	int add0(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	public Object remove(int index) {
		return execute(
			methodCall("remove",new Class[] {Integer.class},new Object[] {new Integer(index)}),
			methodCall("add",new Class[] {Integer.class,Object.class},new Object[] {new Integer(index),null}),1);
	}

	public Object removeImpl(Integer index) {
		return remove0(index.intValue());
	}

	Object remove0(int index) {
		throw new UnsupportedOperationException();
	}

	public int indexOf(Object elem) {
		return ((Integer)execute(
			methodCall("indexOf",new Class[] {Object.class},new Object[] {elem}))).intValue();
	}

	public Integer indexOfImpl(Object elem) {
		return new Integer(indexOf0(elem));
	}

	int indexOf0(Object o) {
	synchronized(mutex()) {
		ListIterator e = PersistentCollections.localList(this).listIterator();
		if (o==null) {
			while (e.hasNext())
				if (e.next()==null)
					return e.previousIndex();
		} else {
			while (e.hasNext())
				if (o.equals(e.next()))
					return e.previousIndex();
		}
		return -1;
	}
	}

	public int lastIndexOf(Object elem) {
		return ((Integer)execute(
			methodCall("lastIndexOf",new Class[] {Object.class},new Object[] {elem}))).intValue();
	}

	public Integer lastIndexOfImpl(Object elem) {
		return new Integer(lastIndexOf0(elem));
	}

	int lastIndexOf0(Object o) {
	synchronized(mutex()) {
		ListIterator e = PersistentCollections.localList(this).listIterator(size());
		if (o==null) {
			while (e.hasPrevious())
				if (e.previous()==null)
					return e.nextIndex();
		} else {
			while (e.hasPrevious())
				if (o.equals(e.previous()))
					return e.nextIndex();
		}
		return -1;
	}
	}

	public void clear() {
	synchronized(mutex()) {
		removeRange(0, size());
	}
	}

	public boolean addAll(int index, RemoteCollection c) {
	synchronized(mutex()) {
		boolean modified = false;
		Iterator e = PersistentCollections.localCollection(c).iterator();
		while (e.hasNext()) {
			add(index++, e.next());
			modified = true;
		}
		return modified;
	}
	}

	public RemoteIterator iterator() throws RemoteException {
		return new Itr();
	}

	public RemoteListIterator listIterator() throws RemoteException {
		return listIterator(0);
	}

	public RemoteListIterator listIterator(final int index) throws RemoteException {
		if (index<0 || index>size())
		  throw new IndexOutOfBoundsException("Index: "+index);

		return new ListItr(index);
	}

	private class Itr extends UnicastRemoteObject implements RemoteIterator {
		int cursor = 0;

		int lastRet = -1;

		int expectedModCount = getModCount();

		Itr() throws RemoteException {}

		public boolean hasNext() {
			return cursor != size();
		}

		public Object next() {
			try {
				Object next = PersistentAbstractList.this.get(cursor);
				checkForComodification();
				lastRet = cursor++;
				return next;
			} catch(IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			if (lastRet == -1)
				throw new IllegalStateException();
			checkForComodification();

			try {
				PersistentAbstractList.this.remove(lastRet);
				if (lastRet < cursor)
					cursor--;
				lastRet = -1;
				expectedModCount = getModCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		final void checkForComodification() {
			if (getModCount() != expectedModCount)
				throw new ConcurrentModificationException();
		}
	}

	private class ListItr extends Itr implements RemoteListIterator {
		ListItr(int index) throws RemoteException {
			cursor = index;
		}

		public boolean hasPrevious() {
			return cursor != 0;
		}

		public Object previous() {
			try {
				Object previous = PersistentAbstractList.this.get(--cursor);
				checkForComodification();
				lastRet = cursor;
				return previous;
			} catch(IndexOutOfBoundsException e) {
				checkForComodification();
				throw new NoSuchElementException();
			}
		}

		public int nextIndex() {
			return cursor;
		}

		public int previousIndex() {
			return cursor-1;
		}

		public void set(Object o) {
			if (lastRet == -1)
				throw new IllegalStateException();
			checkForComodification();

			try {
				PersistentAbstractList.this.set(lastRet, o);
				expectedModCount = getModCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		public void add(Object o) {
			checkForComodification();

			try {
				PersistentAbstractList.this.add(cursor++, o);
				lastRet = -1;
				expectedModCount = getModCount();
			} catch(IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}
	}

	public RemoteList subList(int fromIndex, int toIndex) throws RemoteException {
	synchronized(mutex()) {
		return new SubList(this, fromIndex, toIndex, mutex());
	}
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RemoteList))
			return false;

		ListIterator e1 = PersistentCollections.localList(this).listIterator();
		ListIterator e2 = PersistentCollections.localList((RemoteList) o).listIterator();
		while(e1.hasNext() && e2.hasNext()) {
			Object o1 = e1.next();
			Object o2 = e2.next();
			if (!(o1==null ? o2==null : o1.equals(o2)))
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());
	}

	public int hashCode() {
		int hashCode = 1;
		Iterator i = PersistentCollections.localList(this).iterator();
			 while (i.hasNext()) {
			Object obj = i.next();
			hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

	protected void removeRange(int fromIndex, int toIndex) {
		ListIterator it = PersistentCollections.localList(this).listIterator(fromIndex);
		for (int i=0, n=toIndex-fromIndex; i<n; i++) {
			it.next();
			it.remove();
		}
	}

	public int getModCount() {
		return ((Integer)get("modCount")).intValue();
	}

	public void setModCount(int n) {
		set("modCount",new Integer(n));
	}
}
