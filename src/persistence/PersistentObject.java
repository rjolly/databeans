package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import javax.security.auth.Subject;

public class PersistentObject implements Cloneable, Serializable {
	static final int TIMEOUT=60000;
	persistence.Accessor accessor;
	transient Connection connection;
	transient PersistentClass clazz;
	transient StoreImpl store;
	transient Long base;

	public void init() {}

	static PersistentObject newInstance(long base, PersistentClass clazz, StoreImpl store) {
		try {
			PersistentObject obj=clazz.newInstance();
			obj.base=new Long(base);
			obj.clazz=clazz;
			obj.store=store;
			obj.accessor=obj.createAccessor();
			obj.connection=store.systemConnection;
			return obj;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
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

		public final Object get(String name) {
			return get(clazz.getField(name));
		}

		public final Object set(String name, Object value) {
			return set(clazz.getField(name),value);
		}

		Object get(Field field) {
			return store.get(base.longValue(),field);
		}

		synchronized Object set(Field field, Object value) {
			Object obj=get(field);
			store.set(base.longValue(),field,value);
			return obj;
		}

		synchronized void lock(Transaction transaction) {
			Transaction t=getLock();
			if(t==null) setLock(transaction);
			else if(t==transaction);
			else {
				t=getLock(TIMEOUT);
				if(t==null) setLock(transaction);
				else throw new PersistentException(object()+" locked by "+t);
			}
		}

		synchronized void unlock() {
			setLock(null);
			notify();
		}

		Transaction getLock() {
			return getLock(0);
		}

		Transaction getLock(int timeout) {
			if(timeout>0 && !store.closed) try {
				wait(timeout);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return store.getLock(base.longValue());
		}

		void setLock(Transaction transaction) {
			store.setLock(base.longValue(),transaction);
		}

		synchronized void close() throws RemoteException {
			UnicastRemoteObject.unexportObject(this,true);
		}

		public final long base() {
			return base.longValue();
		}

		public final Store store() {
			return store;
		}

		public final PersistentClass persistentClass() {
			return clazz;
		}

		public int persistentHashCode() {
			return base.hashCode();
		}

		public boolean persistentEquals(PersistentObject obj) {
			return base.equals(obj.base);
		}

		public String persistentToString() {
			return clazz.name()+"@"+Long.toHexString(base.longValue());
		}

		public PersistentObject persistentClone() {
			return ((Accessor)clone()).object();
		}

		public synchronized final Object clone() {
			Accessor obj=(Accessor)store.create(clazz).accessor;
			Iterator t=clazz.fieldIterator();
			while(t.hasNext()) {
				Field field=(Field)t.next();
				obj.set(field,get(field));
			}
			return obj;
		}
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(PersistentClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	protected final PersistentObject create(String name) {
		return connection.create(name);
	}

	protected final PersistentObject create(Class clazz) {
		return connection.create(clazz);
	}

	protected final PersistentObject create(Class clazz, Class types[], Object args[]) {
		return connection.create(clazz,types,args);
	}

	protected final PersistentArray create(Class componentType, int length) {
		return connection.create(componentType,length);
	}

	protected final PersistentArray create(Object component[]) {
		return connection.create(component);
	}

	protected final PersistentClass get(Class clazz) {
		return connection.get(clazz);
	}

	protected final Object get(String name) {
		return execute(
			new MethodCall("get",new Class[] {String.class},new Object[] {name}));
	}

	protected final Object set(String name, Object value) {
		return execute(
			new MethodCall("set",new Class[] {String.class,Object.class},new Object[] {name,value}),
			new MethodCall("set",new Class[] {String.class,Object.class},new Object[] {name,null}),1);
	}

	protected final Object execute(MethodCall call) {
		return connection.execute(call);
	}

	protected final Object execute(MethodCall call, MethodCall undo, int index) {
		return connection.execute(call,undo,index);
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

		Object execute(Subject subject) {
			return execute(PersistentObject.this,subject);
		}

		Object execute(final PersistentObject target, Subject subject) {
			return subject==null?target.call(method,types,args,false):Subject.doAsPrivileged(subject,new PrivilegedAction() {
				public Object run() {
					return target.call(method,types,args,true);
				}
			},null);
		}
	}

	Object call(String method, Class types[], Object args[], boolean check) {
		if(check) {
			if(method.equals("get") || method.equals("set")) AccessController.checkPermission(new PropertyPermission(clazz.name()+"."+args[0]));
			else AccessController.checkPermission(new MethodPermission(clazz.name()+"."+method));
		}
		return ((Accessor)accessor).call(method,types,args);
	}

	void lock(Transaction transaction) {
		((Accessor)accessor).lock(transaction);
	}

	void unlock() {
		((Accessor)accessor).unlock();
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
		return (PersistentClass)execute(
			new MethodCall("persistentClass",new Class[] {},new Object[] {}));
	}

	public int hashCode() {
		return ((Integer)execute(
			new MethodCall("persistentHashCode",new Class[] {},new Object[] {}))).intValue();
	}

	public boolean equals(Object obj) {
		return obj instanceof PersistentObject?equals((PersistentObject)obj):false;
	}

	boolean equals(PersistentObject obj) {
		return ((Boolean)execute(
			new MethodCall("persistentEquals",new Class[] {PersistentObject.class},new Object[] {obj}))).booleanValue();
	}

	public String toString() {
		return (String)execute(
			new MethodCall("persistentToString",new Class[] {},new Object[] {}));
	}

	public Object clone() {
		return execute(
			new MethodCall("persistentClone",new Class[] {},new Object[] {}));
	}

	protected final void finalize() {
		store.release(this);
	}
}
