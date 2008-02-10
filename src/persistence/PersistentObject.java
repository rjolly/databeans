package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;

public class PersistentObject implements Cloneable, Serializable {
	persistence.Accessor accessor;
	transient Connection connection;

	public void init() {}

	protected Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends AccessorImpl {
		public Accessor() throws RemoteException {}

		PersistentObject object() {
			return PersistentObject.this;
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

	AccessorImpl accessor() {
		return (AccessorImpl)accessor;
	}

	Object call(String method, Class types[], Object args[], boolean check) {
		return accessor().call(method,types,args,check);
	}

	void lock(Transaction transaction) {
		accessor().lock(transaction.accessor());
	}

	void unlock() {
		accessor().unlock();
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
}
