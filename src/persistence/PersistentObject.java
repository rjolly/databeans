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
	transient long base;

	public void init() {}

	static PersistentObject newInstance(long base, PersistentClass clazz, StoreImpl store) {
		PersistentObject obj=clazz.newInstance();
		obj.init(base,clazz,store);
		return obj;
	}

	void init(long base, PersistentClass clazz, StoreImpl store) {
		this.base=base;
		this.clazz=clazz;
		this.store=store;
		try {
			accessor=createAccessor();
			connection=store.systemConnection;
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

			public Store store() throws RemoteException {
				return PersistentObject.this.store;
			}

			public int hashCode() {
				return new Long(PersistentObject.this.base).hashCode();
			}

			public boolean equals(Object obj) {
				return obj instanceof Accessor && PersistentObject.this.base==((Accessor)obj).object().base;
			}
		};
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
			return store.get(base,field);
		}

		synchronized Object set(Field field, Object value) {
			Object obj=get(field);
			store.set(base,field,value);
			return obj;
		}

		synchronized void lock(Transaction transaction) {
			Transaction t=getLock();
			if(t==null) setLock(transaction);
			else if(t==transaction);
			else {
				t=getLock(TIMEOUT);
				if(t==null) setLock(transaction);
				else throw new PersistentException(this+" locked by "+t);
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
			return store.getLock(base);
		}

		void setLock(Transaction transaction) {
			store.setLock(base,transaction);
		}

		synchronized void close() throws RemoteException {
			UnicastRemoteObject.unexportObject(this,true);
		}

		public final long base() {
			return base;
		}

		public final Store store() {
			return store;
		}

		public final PersistentClass persistentClass() {
			return clazz;
		}

		public int persistentHashCode() {
			return hashCode();
		}

		public final int hashCode() {
			return new Long(base).hashCode();
		}

		public boolean persistentEquals(PersistentObject obj) {
			return equals(obj.accessor);
		}

		public final boolean equals(Object obj) {
			return this == obj || (obj instanceof Accessor && equals((Accessor)obj));
		}

		boolean equals(Accessor accessor) {
			return base==accessor.object().base;
		}

		public String persistentToString() {
			return toString();
		}

		public final String toString() {
			return clazz.name()+"@"+Long.toHexString(base);
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

		protected final void finalize() {
			store.release(object());
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
			if((method.equals("get") || method.equals("set")) && types.length>0 && types[0]==String.class && !unchecked((String)args[0])) AccessController.checkPermission(new PropertyPermission(clazz.name()+"."+args[0]));
			else AccessController.checkPermission(new MethodPermission(clazz.name()+"."+method));
		}
		return ((Accessor)accessor).call(method,types,args);
	}

	protected boolean unchecked(String property) {
		return false;
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
		return this == obj || (obj instanceof PersistentObject && equals((PersistentObject)obj));
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
}
