package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.security.auth.Subject;
import persistence.PersistentObject.MethodCall;

class RemoteConnection extends UnicastRemoteObject {
	final Connection connection;
	final Store store;
	int level;
	boolean readOnly;
	boolean autoCommit;
	Subject subject;

	RemoteConnection(Connection connection, Store store, boolean readOnly, Subject subject) throws RemoteException {
		this(connection,store,Connection.TRANSACTION_NONE,readOnly,subject);
	}

	RemoteConnection(Connection connection, Store store, int level, boolean readOnly, Subject subject) throws RemoteException {
		this.connection=connection;
		this.store=store;
		this.level=level;
		this.readOnly=readOnly;
		this.subject=subject;
		open();
	}

	void open() {
		store.connections.put(this,null);
	}

	public PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
		return create((PersistentClass)store.attach(clazz),types,store.attach(args),true);
	}

	synchronized PersistentObject create(PersistentClass clazz, Class types[], Object args[], boolean attached) {
		if(readOnly) throw new RuntimeException("read only");
		try {
			PersistentObject obj=store.create(clazz);
			obj.getClass().getMethod("init",types).invoke(obj,args);
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized PersistentClass get(String name) {
		try {
			return get(Class.forName(name));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized PersistentClass get(Class clazz) {
		return store.get(clazz);
	}

	public synchronized PersistentClass get(Class componentType, int length) {
		return store.get(componentType,length);
	}

	public PersistentSystem system() {
		return store.system;
	}

	public Subject subject() {
		return subject;
	}

	public int getTransactionIsolation() {
		return level;
	}

	public void setTransactionIsolation(int level) {
		this.level=level;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly=readOnly;
	}

	public boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit=autoCommit;
	}

	public Object execute(MethodCall call) {
		return connection.attach(store.attach(call)).execute();
	}

	public Object executeAtomic(MethodCall call) {
		return execute(store.attach(call),null,0,true);
	}

	public Object executeAtomic(MethodCall call, MethodCall undo, int index) {
		return execute(store.attach(call),store.attach(undo),index,false);
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(!read && readOnly) throw new RuntimeException("read only");
		Object obj=call.execute(subject);
		return obj;
	}
}
