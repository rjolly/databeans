package persistence;

import java.io.Serializable;
import java.util.Iterator;

public class PersistentObject implements Cloneable, Serializable {
	persistence.Accessor accessor;
	Connection connection;
	transient PersistentClass clazz;
	transient long base;

	public void init() {}

	protected Accessor createAccessor() {
		return new Accessor();
	}

	protected class Accessor extends AccessorImpl {
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
			new MethodCall(this,"get",new Class[] {String.class},new Object[] {name}));
	}

	protected final Object set(String name, Object value) {
		return execute(
			new MethodCall(this,"set",new Class[] {String.class,Object.class},new Object[] {name,value}),
			new MethodCall(this,"set",new Class[] {String.class,Object.class},new Object[] {name,null}),1);
	}

	protected final Object execute(MethodCall call) {
		return connection.execute(call);
	}

	protected final Object execute(MethodCall call, MethodCall undo, int index) {
		return connection.execute(call,undo,index);
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
		return new Long(base()).hashCode();
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof PersistentObject && base()==((PersistentObject)obj).base());
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

	public final long base() {
		return base==0?base=accessor.base():base;
	}

	public final PersistentClass persistentClass() {
		return clazz==null?clazz=accessor.persistentClass():clazz;
	}

	public Object clone() {
		return connection.create(this);
	}
}
