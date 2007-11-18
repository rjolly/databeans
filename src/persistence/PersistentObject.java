package persistence;

import java.rmi.*;
import java.rmi.server.*;
import java.security.PrivilegedAction;

public abstract class PersistentObject extends UnicastRemoteObject {
	Accessor accessor;
	ConnectionImpl connection;
	Peer peer;
	Long base;

	public PersistentObject() throws RemoteException {}

	public PersistentObject(Accessor accessor, Connection connection) throws RemoteException {
		init(accessor,connection);
	}

	void init(Accessor accessor, Connection connection) {
		this.accessor=accessor;
		this.connection=(ConnectionImpl)connection;
		peer=new Peer(ref);
		base=new Long(accessor.base);
	}

	protected final Remote create(String name) {
		return connection.create(name);
	}

	protected final Remote create(Class clazz) {
		return connection.create(clazz);
	}

	protected final Remote create(Class clazz, Class types[], Object args[]) {
		return connection.create(clazz,types,args);
	}

	protected final RemoteArray create(Class componentType, int length) {
		return connection.create(componentType,length);
	}

	protected final RemoteArray create(Object component[]) {
		return connection.create(component);
	}

	protected final Object get(final String name) {
		return connection.execute(accessor,
			new PrivilegedAction() {
				public Object run() {
					Field field=accessor.clazz.getField(name);
					return get(field);
				}
			}
		);
	}

	protected final void set(final String name, final Object value) {
		connection.execute(accessor,
			new PrivilegedAction() {
				public Object run() {
					Field field=accessor.clazz.getField(name);
					Object obj=get(field);
					set(field,value);
					connection.record(PersistentObject.this,"set",new Class[] {String.class,Object.class},new Object[] {name,obj});
					return obj;
				}
			}
		);
	}

	Object get(Field field) {
		return connection.attach(accessor.get(field));
	}

	void set(Field field, Object value) {
		accessor.set(field,connection.detach(value));
	}

	public final String toString() {
		try {
			return remoteToString();
		} catch (RemoteException e) {
			throw new RuntimeException();
		}
	}

	public String remoteToString() throws RemoteException {
		StringBuffer s=new StringBuffer();
		Object obj;
		s.append("[");
		Field fields[]=accessor.clazz.fields;
		for(int i=0;i<fields.length;i++) s.append((i==0?"":", ")+fields[i].name+"="+((obj=get(fields[i]))==this?"this":obj));
		s.append("]");
		return s.toString();
	}

	public final String persistentClass() {
		return accessor.clazz.toString();
	}
}

class Peer extends RemoteObject {
	Peer(RemoteRef ref) {
		super(ref);
	}
}
