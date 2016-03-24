package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import javax.security.auth.Subject;

public class PersistentObject implements Cloneable, Serializable {
	persistence.Accessor accessor;
	transient PersistentClass clazz;
	transient Store store;
	transient long base;

	public void init() {}

	static PersistentObject newInstance(long base, PersistentClass clazz, Store store) {
		PersistentObject obj=clazz.newInstance();
		obj.init(base,clazz,store);
		return obj;
	}

	void init(long base, PersistentClass clazz, Store store) {
		this.base=base;
		this.clazz=clazz;
		this.store=store;
		try {
			accessor=createAccessor();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	static PersistentObject newInstance(long base) {
		PersistentObject obj=new PersistentObject();
		obj.init(base);
		return obj;
	}

	void init(long base) {
		this.base=base;
		accessor=new persistence.Accessor() {
			public long base() {
				return PersistentObject.this.base;
			}

			public PersistentClass clazz() {
				return clazz;
			}

			public Store store() {
				return store;
			}

			public int hashCode() {
				return new Long(PersistentObject.this.base).hashCode();
			}

			public boolean equals(Object obj) {
				return obj instanceof Accessor && PersistentObject.this.base==((Accessor)obj).object().base;
			}
		};
	}

	void init(PersistentObject object) {
		accessor=object.accessor;
	}

	protected Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends UnicastRemoteObject implements persistence.Accessor {
		public Accessor() throws RemoteException {}

		PersistentObject object() {
			return PersistentObject.this;
		}

		Object call(String method, Class types[], Object args[]) {
			try {
				return getClass().getMethod(method,types).invoke(this,args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public final long base() {
			return base;
		}

		public final PersistentClass clazz() {
			return clazz;
		}

		public final Store store() {
			return store;
		}

		protected final void finalize() {
			store.release(PersistentObject.this);
		}
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(PersistentClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	protected final PersistentObject create(String name) {
		return store.create(name);
	}

	protected final PersistentObject create(Class clazz) {
		return store.create(clazz);
	}

	protected final PersistentObject create(Class clazz, Class types[], Object args[]) {
		return store.create(clazz,types,args);
	}

	protected final PersistentArray create(Class componentType, int length) {
		return store.create(componentType,length);
	}

	protected final PersistentArray create(Object component[]) {
		return store.create(component);
	}

	protected final PersistentClass get(Class clazz) {
		return store.get(clazz);
	}

	public final Object get(String name) {
		return get(clazz.getField(name));
	}

	public final Object set(String name, Object value) {
		return set(clazz.getField(name),value);
	}

	Object get(Field field) {
		return store.get(base,field);
	}

	synchronized Object set(Field field, Object value) {
		Object obj=get(field);
		store.set(base,field,value);
		return obj;
	}

	protected final Object execute(MethodCall call) {
		return store.execute(call);
	}

	protected final Object executeAtomic(MethodCall call) {
		return store.executeAtomic(call);
	}

	protected final Object executeAtomic(MethodCall call, MethodCall undo, int index) {
		return store.executeAtomic(call,undo,index);
	}

	protected final class MethodCall implements Serializable {
		String method;
		Class types[];
		Object args[];

		public MethodCall(String method, Class types[], Object args[]) {
			this.method=method;
			this.types=types;
			this.args=args;
		}

		PersistentObject target() {
			return PersistentObject.this;
		}

		Object execute() {
			return call(method,types,args);
		}

		Object execute(Subject subject) {
			return execute(PersistentObject.this,subject);
		}

		Object execute(final PersistentObject target, Subject subject) {
			return target.call(method,types,args,false);
		}
	}

	Object call(String method, Class types[], Object args[]) {
		try {
			return getClass().getMethod(method,types).invoke(this,args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	Object call(String method, Class types[], Object args[], boolean check) {
		return ((Accessor)accessor).call(method,types,args);
	}

	void close() {
		accessor=null;
	}

	long base() {
		try {
			return accessor.base();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	Store store() {
		try {
			return accessor.store();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public final PersistentClass persistentClass() {
		return clazz==null?clazz=getClass(accessor):clazz;
	}

	PersistentClass getClass(persistence.Accessor accessor) {
		try {
			return accessor.clazz();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public final int hashCode() {
		return new Long(base).hashCode();
	}

	public final boolean equals(Object obj) {
		return this == obj || (obj instanceof PersistentObject && equals((PersistentObject)obj));
	}

	boolean equals(PersistentObject obj) {
		return base==obj.base;
	}

	public String toString() {
		return clazz.name()+"@"+Long.toHexString(base);
	}

	public synchronized final Object clone() {
		PersistentObject obj=store.create(clazz);
		Iterator t=clazz.fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			obj.set(field,get(field));
		}
		return obj;
	}
}
