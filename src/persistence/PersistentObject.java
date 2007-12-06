package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Iterator;

public class PersistentObject implements Cloneable, Serializable {
	private persistence.Accessor accessor;
	private Connection connection;
	transient PersistentClass clazz;
	transient Long base;

	public void init() {}

	protected Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends AccessorImpl {
		protected Accessor() throws RemoteException {}

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
		try {
			return connection.create(name);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected final PersistentObject create(Class clazz) {
		try {
			return connection.create(clazz);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected final PersistentObject create(Class clazz, Class types[], Object args[]) {
		try {
			return connection.create(clazz,types,args);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected final PersistentArray create(Class componentType, int length) {
		try {
			return connection.create(componentType,length);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected final PersistentArray create(Object component[]) {
		try {
			return connection.create(component);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected final Object get(String name) {
		return execute(
			new MethodCall(this,"get",new Class[] {String.class},new Object[] {name}));
	}

	protected final Object set(String name, Object value) {
		return execute(
			new MethodCall(this,"set",new Class[] {String.class,Object.class},new Object[] {name,value}),
			new MethodCall(this,"set",new Class[] {String.class,Object.class},new Object[] {name,null}),1);
	}

	protected final Object execute(MethodCall call) {
		try {
			return connection.execute(call);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	protected final Object execute(MethodCall call, MethodCall undo, int index) {
		try {
			return connection.execute(call,undo,index);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
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

	public int hashCode() {
		return base().hashCode();
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof PersistentObject && base().equals(((PersistentObject)obj).base()));
	}

	public String toString() {
		StringBuffer s=new StringBuffer();
		s.append("[");
		Iterator t=persistentClass().fieldIterator();
		while(t.hasNext()) {
			Field field=(Field)t.next();
			Object obj=get(field.name);
			s.append(field.name+"="+(equals(obj)?"this":obj)+(t.hasNext()?", ":""));
		}
		s.append("]");
		return s.toString();
	}

	public final Long base() {
		try {
			return base==null?base=accessor.base():base;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public final PersistentClass persistentClass() {
		try {
			return clazz==null?clazz=accessor.persistentClass():clazz;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object clone() {
		return execute(
			new MethodCall(this,"copy",new Class[] {},new Object[] {}));
	}
}
