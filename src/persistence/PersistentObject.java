package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;

public class PersistentObject implements Cloneable, Serializable {
	persistence.Accessor accessor;
	Connection connection;

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
			new MethodCall("get",new Class[] {String.class},new Object[] {name}));
	}

	protected final Object set(String name, Object value) {
		return execute(
			new MethodCall("set",new Class[] {String.class,Object.class},new Object[] {name,value}),
			new MethodCall("set",new Class[] {String.class,Object.class},new Object[] {name,null}),1);
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

		MethodCall attach(StoreImpl store) {
			return PersistentObject.this.attach(store).new MethodCall(method,types,PersistentObject.attach(store,args));
		}

		Object execute() {
			return execute(PersistentObject.this);
		}

		Object execute(PersistentObject target) {
			return target.call(method,types,args);
		}

		public String toString() {
			return toHexString()+"."+method+java.util.Arrays.asList(args);
		}
	}

	static Object attach(Connection connection, Object obj) {
		return obj instanceof PersistentObject?((PersistentObject)obj).attach(connection):obj;
	}

	static Object attach(StoreImpl store, Object obj) {
		return obj instanceof PersistentObject?((PersistentObject)obj).attach(store):obj;
	}

	static Object[] attach(StoreImpl store, Object obj[]) {
		Object a[]=new Object[obj.length];
		for(int i=0;i<obj.length;i++) a[i]=attach(store,obj[i]);
		return a;
	}

	static Object execute(PersistentMethodCall call) {
		Array t=call.getTypes();
		Array a=call.getArgs();
		Class types[]=new Class[t.length()];
		Object args[]=new Object[a.length()];
		Arrays.copy(t,0,types,0,types.length);
		Arrays.copy(a,0,args,0,args.length);
		return call.getTarget().new MethodCall(call.getMethod(),types,args).execute();
	}

	AccessorImpl accessor() {
		return (AccessorImpl)accessor;
	}

	PersistentObject attach(StoreImpl store) {
		try {
			if(!store.equals(accessor.store())) throw new PersistentException("not the same store");
			return store.get(accessor.base()).object();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	PersistentObject attach(Connection connection) {
		return accessor().object(connection);
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

	public final PersistentClass persistentClass() {
		try {
			return clazz==null?clazz=accessor.persistentClass():clazz;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	private transient PersistentClass clazz;

	public int hashCode() {
		return ((Integer)execute(
			new MethodCall("hashCode",new Class[] {},new Object[] {}))).intValue();
	}

	public boolean equals(Object obj) {
		return ((Boolean)execute(
			new MethodCall("equals",new Class[] {Object.class},new Object[] {obj}))).booleanValue();
	}

	public String toHexString() {
		return (String)execute(
			new MethodCall("toHexString",new Class[] {},new Object[] {}));
	}

	public String toString() {
		return (String)execute(
			new MethodCall("toString",new Class[] {},new Object[] {}));
	}

	public Object clone() {
		return execute(
			new MethodCall("copy",new Class[] {},new Object[] {}));
	}
}
