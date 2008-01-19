package persistence;

import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.Principal;
import javax.security.auth.Subject;
import persistence.PersistentObject.MethodCall;
import persistence.server.DatabeansPrincipal;

abstract class RemoteConnectionImpl extends UnicastRemoteObject implements RemoteConnection {
	final StoreImpl store;
	Transaction transaction;
	int level;
	boolean readOnly;
	boolean autoCommit;
	Subject subject;

	RemoteConnectionImpl(StoreImpl store, int level, boolean readOnly, Subject subject) throws RemoteException {
		this.store=store;
		this.level=level;
		this.readOnly=readOnly;
		this.subject=subject;
		if(level!=Connection.TRANSACTION_NONE) transaction=store.getTransaction(client());
		open();
	}

	void open() {
		store.connections.put(this,null);
	}

	String client() {
		String name=((Principal)subject.getPrincipals(DatabeansPrincipal.class).iterator().next()).getName();
		try {
			name+="@"+getClientHost();
		} catch (ServerNotActiveException e) {}
		return name;
	}

	abstract Connection connection();

	public PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
		return create((PersistentClass)store.attach(clazz),types,store.attach(args),true);
	}

	synchronized PersistentObject create(PersistentClass clazz, Class types[], Object args[], boolean attached) {
		try {
			PersistentObject obj=store.create(clazz).object();
			obj.getClass().getMethod("init",types).invoke(obj,args);
			return obj;
		} catch (Exception e) {
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
		return execute(store.attach(call),null,0,true);
	}

	public Object execute(MethodCall call, MethodCall undo, int index) {
		return execute(store.attach(call),store.attach(undo),index,false);
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(!read && readOnly) throw new PersistentException("read only");
		Object obj=transaction!=null?transaction.execute(call,undo,index,level,read,readOnly,subject):call.execute(subject);
		if(autoCommit) commit();
		return obj;
	}

	public synchronized void commit() {
		if(transaction!=null) transaction.commit(subject);
	}

	public synchronized void rollback() {
		if(transaction!=null) transaction.rollback(subject);
	}

	synchronized void close() throws RemoteException {
		UnicastRemoteObject.unexportObject(this,true);
		connection().close();
	}

	protected final void finalize() {
		if(transaction!=null) store.release(transaction,subject);
	}
}
