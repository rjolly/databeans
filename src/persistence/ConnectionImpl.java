package persistence;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.RemoteStub;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.Principal;
import java.util.Map;
import java.util.WeakHashMap;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class ConnectionImpl extends UnicastRemoteObject implements Connection {
	final StoreImpl store;
	final Map cache=new WeakHashMap();
	final Map refCache=new WeakHashMap();
	Transaction transaction;
	boolean autoCommit;
	boolean readOnly;
	boolean closed;
	String name;
	int level;

	ConnectionImpl(StoreImpl store, int level, Principal user) throws RemoteException {
		this.store=store;
		this.level=level;
		name=user.getName();
		try {
			name+="@"+RemoteServer.getClientHost();
		} catch (ServerNotActiveException e) {}
		if(level!=TRANSACTION_NONE) transaction=store.getTransaction(name);
		open();
	}

	void open() {
		if(transaction!=null) store.transactions.add(transaction);
		store.connections.add(this);
	}

	public Remote create(String name) {
		try {
			return create(Class.forName(name));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Remote create(Class clazz) {
		return create(clazz,new Class[] {},new Object[] {});
	}

	public Remote create(Class clazz, Class types[], Object args[]) {
		return create(new PersistentClass(clazz),types,args);
	}

	public RemoteArray create(Class componentType, int length) {
		return (RemoteArray)create(new ArrayClass(componentType,length),new Class[] {},new Object[] {});
	}

	public RemoteArray create(Object component[]) {
		Class componentType=component.getClass().getComponentType();
		int length=component.length;
		return (RemoteArray)create(new ArrayClass(componentType,length),new Class[] {Object[].class},new Object[] {component});
	}

	synchronized PersistentObject create(PersistentClass c, Class types[], Object args[]) {
		if(closed) throw new PersistentException("connection closed");
		return cache(c.newInstance(store.create(c),this,types,args));
	}

	PersistentObject cache(PersistentObject obj) {
		synchronized(cache) {
			cache.put(obj.base,new WeakReference(obj));
			refCache.put(obj.peer,new WeakReference(obj));
			return obj;
		}
	}

	PersistentObject instantiate(Accessor a) {
		synchronized(cache) {
			PersistentObject obj;
			Reference w;
			if((obj=(w=(Reference)cache.get(new Long(a.base)))==null?null:(PersistentObject)w.get())==null) {
				obj=a.clazz.newInstance(a,this);
				cache.put(obj.base,new WeakReference(obj));
				refCache.put(obj.peer,new WeakReference(obj));
			}
			return obj;
		}
	}

	Object attach(Object obj) {
		return obj instanceof Accessor?instantiate((Accessor)obj):obj;
	}

	Object attach(ConnectionImpl connection, Object obj) {
		return connection==this?obj:attach(connection.detach(obj));
	}

	Object detach(Object obj) {
		if(obj instanceof PersistentObject) {
			PersistentObject b;
			if((b=(PersistentObject)obj).connection==this) return b.accessor;
			else throw new PersistentException("not the same connection");
		} else if(obj instanceof RemoteStub) synchronized(cache) {
			Reference w;
			return (w=(Reference)refCache.get(obj))==null?obj:((PersistentObject)w.get()).accessor;
		} else return obj;
	}

	public Object getRoot() {
		return store.getSystem(this).getRoot();
	}

	public void setRoot(Object obj) {
		store.getSystem(this).setRoot(obj);
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

	MethodCall methodCall(PersistentObject target, String method, Class types[], Object args[]) {
		return store.methodCall(this,target,method,types,args);
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(closed) throw new PersistentException("connection closed");
		if(!read && readOnly) throw new PersistentException("read only");
		if(transaction!=null && level==TRANSACTION_SERIALIZABLE && !(read && readOnly)) transaction.lock(call.target);
		Object obj=call.execute();
		if(transaction!=null && !read) {
			undo.args[index]=obj;
			transaction.record(undo);
		}
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
