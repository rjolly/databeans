package persistence;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.Principal;
import java.security.PrivilegedAction;
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
		if(level!=Connection.TRANSACTION_NONE) transaction=store.getTransaction(clientName()+"@"+clientHost());
		synchronized(store.connections) {
			store.connections.put(this,null);
		}
	}

	String clientName() {
		return ((Principal)subject.getPrincipals(DatabeansPrincipal.class).iterator().next()).getName();
	}

	String clientHost() {
		String host="";
		try {
			host=RemoteServer.getClientHost();
		} catch (ServerNotActiveException e) {}
		return host;
	}

	abstract Connection connection();

	public PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
		try {
			PersistentObject obj=store.create(clazz).object();
			obj.getClass().getMethod("init",types).invoke(obj,args);
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public PersistentSystem getSystem() {
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

	Object execute(final MethodCall call, final MethodCall undo, final int index, final boolean read) {
		if(!read && readOnly) throw new PersistentException("read only");
		Object obj=Subject.doAsPrivileged(subject,new PrivilegedAction() {
			public Object run() {
				return transaction!=null?transaction.execute(call,undo,index,level,read,readOnly):call.execute();
			}
		},null);
		if(autoCommit) commit();
		return obj;
	}

	public void commit() {
		if(transaction!=null) transaction.commit();
	}

	public void rollback() {
		if(transaction!=null) transaction.rollback();
	}

	void close() throws RemoteException {
		if(transaction!=null) transaction.kick();
		UnicastRemoteObject.unexportObject(this,true);
		connection().close();
	}

	protected final void finalize() {
		if(transaction!=null) store.release(transaction);
	}
}
