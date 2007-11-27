package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;

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
		synchronized(connection) {
			connection.begin(true);
			Object obj=connection.call(this,"get",new Class[] {String.class},new Object[] {name});
			connection.record(this);
			connection.autoCommit();
			return obj;
		}
	}

	protected final Object set(String name, Object value) {
		synchronized(connection) {
			connection.begin(false);
			Object obj=connection.call(this,"set",new Class[] {String.class,Object.class},new Object[] {name,value});
			connection.record(this,"set",new Class[] {String.class,Object.class},new Object[] {name,obj});
			connection.autoCommit();
			return obj;
		}
	}

	void lock(PersistentObject transaction) {
		accessor.lock(transaction.accessor);
	}

	void unlock() {
		if(accessor.getLock()!=null) accessor.unlock();
	}

	Object call(String method, Class types[], Object args[]) {
		try {
			return getClass().getMethod(method+"Impl",types).invoke(this,args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	Object getImpl(String name) {
		return get(accessor.clazz.getField(name));
	}

	Object setImpl(String name, Object value) {
		return set(accessor.clazz.getField(name),value);
	}

	Object get(Field field) {
		return connection.attach(accessor.get(field));
	}

	Object set(Field field, Object value) {
		Object obj=connection.attach(accessor.get(field));
		accessor.set(field,connection.detach(value));
		return obj;
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
