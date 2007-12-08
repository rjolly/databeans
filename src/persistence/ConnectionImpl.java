package persistence;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import persistence.PersistentObject.MethodCall;

public class ConnectionImpl extends UnicastRemoteObject implements Connection {
	final StoreImpl store;
	Transaction transaction;
	boolean autoCommit;
	boolean readOnly;
	boolean closed;
	int level;
	Subject subject;
	String clientHost="";

	ConnectionImpl(StoreImpl store, int level, Subject subject) throws RemoteException {
		this.store=store;
		this.level=level;
		this.subject=subject;
		try {
			clientHost=RemoteServer.getClientHost();
		} catch (ServerNotActiveException e) {}
		if(level!=TRANSACTION_NONE) transaction=store.getTransaction(subject+"@"+clientHost);
		open();
	}

	void open() {
		if(transaction!=null) store.transactions.add(transaction);
		store.connections.add(this);
	}

	public PersistentObject create(String name) {
		try {
			return create(Class.forName(name));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public PersistentObject create(Class clazz) {
		return create(clazz,new Class[] {},new Object[] {});
	}

	public PersistentObject create(Class clazz, Class types[], Object args[]) {
		return create(new PersistentClass(clazz),types,args);
	}

	public PersistentArray create(Class componentType, int length) {
		return (PersistentArray)create(new ArrayClass(componentType,length),new Class[] {},new Object[] {});
	}

	public PersistentArray create(Object component[]) {
		Class componentType=component.getClass().getComponentType();
		int length=component.length;
		return (PersistentArray)create(new ArrayClass(componentType,length),new Class[] {Object[].class},new Object[] {component});
	}

	synchronized PersistentObject create(PersistentClass c, Class types[], Object args[]) {
		if(closed) throw new PersistentException("connection closed");
		try {
			PersistentObject obj=store.create(c).object(this);
			obj.getClass().getMethod("init",types).invoke(obj,args);
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public PersistentSystem getSystem() {
		return store.getSystem(this);
	}

	public Object getRoot() throws RemoteException {
		return getSystem().getRoot();
	}

	public void setRoot(Object obj) throws RemoteException {
		getSystem().setRoot(obj);
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
		return PersistentObject.attach(this,execute(call.attach(store),null,0,true));
	}

	public Object execute(MethodCall call, MethodCall undo, int index) {
		return PersistentObject.attach(this,execute(call.attach(store),undo.attach(store),index,false));
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(closed) throw new PersistentException("connection closed");
		if(!read && readOnly) throw new PersistentException("read only");
		Object obj;
		if(transaction!=null) {
			obj=call.execute(transaction.copy(call.target(),level,read,readOnly));
			if(!read) {
				undo.args[index]=obj;
				transaction.record(call,undo,level);
			}
		} else obj=call.execute();
		if(autoCommit) commit();
		return obj;
	}

	public synchronized void commit() {
		if(closed) throw new PersistentException("connection closed");
		if(transaction!=null) transaction.commit();
	}

	public synchronized void rollback() {
		if(closed) throw new PersistentException("connection closed");
		if(transaction!=null) transaction.rollback();
	}

	void kick() {
		if(transaction!=null) transaction.kick();
	}

	public void close() {
		close(false);
	}

	synchronized void close(boolean force) {
		if(closed) throw new PersistentException("connection closed");
		if(!force) rollback();
		store.connections.remove(this);
		if(transaction!=null) store.transactions.remove(transaction);
		closed=true;
	}

	public boolean isClosed() {
		return closed;
	}

	public XAResource getXAResource() {
		return new ConnectionXAResource(this);
	}
}

class ConnectionXAResource implements XAResource, Serializable {
	Connection connection;

	ConnectionXAResource(Connection connection) {
		this.connection=connection;
	}

	public void commit(Xid xid, boolean onePhase) {
		try {
			connection.commit();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void end(Xid xid, int flags) {}

	public void forget(Xid xid) {}

	public int getTransactionTimeout() {
		return 0;
	}

	public boolean isSameRM(XAResource xares) {
		return equals(xares);
	}

	public final int hashCode() {
		return connection.hashCode();
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof ConnectionXAResource && connection.equals(((ConnectionXAResource)obj).connection));
	}

	public int prepare(Xid xid) {
		return XA_OK;
	}

	public Xid[] recover(int flag) {
		return new Xid[] {};
	}

	public void rollback(Xid xid) {
		try {
			connection.rollback();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean setTransactionTimeout(int seconds) {
		return seconds==0;
	}

	public void start(Xid xid, int flags) {}
}
