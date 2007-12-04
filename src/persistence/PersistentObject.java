package persistence;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Iterator;

public class PersistentObject implements Cloneable, Serializable {
	Remote accessor;
	Connection connection;
	PersistentClass clazz;
	long base;

	protected void init() {}

	protected PersistentObject() {}

	protected Accessor accessor() throws RemoteException {
		return new Accessor(this);
	}

	void init(Accessor accessor, Connection connection) {
		this.accessor=accessor;
		this.connection=connection;
		clazz=accessor.clazz;
		base=accessor.base.longValue();
	}

	public final PersistentObject create(String name) {
		return connection.create(name);
	}

	public final PersistentObject create(Class clazz) {
		return connection.create(clazz);
	}

	public final PersistentObject create(Class clazz, Class types[], Object args[]) {
		return connection.create(clazz,types,args);
	}

	public final PersistentArray create(Class componentType, int length) {
		return connection.create(componentType,length);
	}

	public final PersistentArray create(Object component[]) {
		return connection.create(component);
	}

	protected final Object get(String name) {
		return execute(
			new MethodCall(this,"get",new Class[] {String.class},new Object[] {name}));
	}

	protected final void set(String name, Object value) {
		execute(
			new MethodCall(this,"set",new Class[] {String.class,Object.class},new Object[] {name,value}),
			new MethodCall(this,"set",new Class[] {String.class,Object.class},new Object[] {name,null}),1);
	}

	protected final Object execute(MethodCall call) {
		return connection.execute(call,null,0,true);
	}

	protected final Object execute(MethodCall call, MethodCall undo, int index) {
		return connection.execute(call,undo,index,false);
	}

	void lock(Transaction transaction) {
		((Accessor)accessor).lock((Accessor)transaction.accessor);
	}

	void unlock() {
		((Accessor)accessor).unlock();
	}

	void kick() {
		((Accessor)accessor).kick();
	}

	public int hashCode() {
		return new Long(base).hashCode();
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof PersistentObject && base==((PersistentObject)obj).base);
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

	public final PersistentClass persistentClass() {
		return clazz;
	}

	public Object clone() {
		return connection.create(this);
	}
}
