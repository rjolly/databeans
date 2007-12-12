package persistence;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.Principal;
import javax.security.auth.Subject;
import persistence.PersistentObject.MethodCall;
import persistence.server.DatabeansPrincipal;

public class RemoteConnectionImpl extends UnicastRemoteObject implements RemoteConnection {
	final StoreImpl store;
	Transaction transaction;
	boolean autoCommit;
	boolean readOnly;
	int level;
	Subject subject;

	RemoteConnectionImpl(StoreImpl store, int level, Subject subject) throws RemoteException {
		this.store=store;
		this.level=level;
		this.subject=subject;
		if(level!=Connection.TRANSACTION_NONE) transaction=store.getTransaction(clientName()+"@"+clientHost());
		open();
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

	void open() {
		if(transaction!=null) store.transactions.add(transaction);
		store.connections.add(this);
	}

	public synchronized PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
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

	public boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit=autoCommit;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly=readOnly;
	}

	public Object execute(MethodCall call) {
		return execute(store.attach(call),null,0,true);
	}

	public Object execute(MethodCall call, MethodCall undo, int index) {
		return execute(store.attach(call),store.attach(undo),index,false);
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(!read && readOnly) throw new PersistentException("read only");
		Object obj=transaction!=null?transaction.execute(call,undo,index,level,read,readOnly):call.execute();
		if(autoCommit) commit();
		return obj;
	}

	public synchronized void commit() {
		if(transaction!=null) transaction.commit();
	}

	public synchronized void rollback() {
		if(transaction!=null) transaction.rollback();
	}

	public synchronized void close(boolean force) throws RemoteException {
		if(!force) rollback();
		store.connections.remove(this);
		if(transaction!=null) {
			transaction.kick();
			store.transactions.remove(transaction);
		}
		UnicastRemoteObject.unexportObject(this,true);
	}
}
