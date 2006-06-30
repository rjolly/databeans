package persistence;

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

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

	protected final Object get(String name) {
		return get(accessor.clazz.getField(name));
	}

	protected final void set(String name, Object value) {
		set(accessor.clazz.getField(name),value);
	}

	Object get(Field field) {
		Object obj;
		Accessor a=connection.copy(accessor,true);
		obj=connection.attach(a.get(field));
		connection.autoCommit();
		return obj;
	}

	void set(Field field, Object value) {
		Accessor a=connection.copy(accessor,false);
		a.set(field,connection.detach(value));
		connection.autoCommit();
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
