package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Iterator;

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

	protected PersistentObject() {}

	void init(persistence.Accessor accessor, Connection connection) {
		this.accessor=accessor;
		this.connection=connection;
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

		Object execute() {
			return execute(PersistentObject.this);
		}

		Object execute(PersistentObject target) {
			return target.call(method,types,args);
		}
	}

	AccessorImpl accessor() {
		return (AccessorImpl)accessor;
	}

	Object call(String method, Class types[], Object args[]) {
		return accessor().call(method,types,args);
	}

	void lock(Transaction transaction) {
		accessor().lock(transaction.accessor());
	}

	void unlock() {
		accessor().unlock();
	}

	void kick() {
		accessor().kick();
	}

	Long base() {
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
		try {
			return accessor.persistentClass();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public int hashCode() {
		try {
			return accessor.remoteHashCode();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean equals(Object obj) {
		try {
			return obj instanceof PersistentObject?((PersistentObject)obj).accessor.remoteEquals(this):false;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		try {
			return accessor.remoteToString();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public String dump() {
		StringBuffer s=new StringBuffer();
		s.append("[");
		Iterator t=persistentClass().fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			Object obj=get(field.name);
			s.append(field.name+"="+obj+(t.hasNext()?", ":""));
		}
		s.append("]");
		return s.toString();
	}

	public Object clone() {
		try {
			return connection.attach(accessor.remoteClone());
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
}
