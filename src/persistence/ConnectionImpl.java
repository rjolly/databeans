package persistence;

import java.io.*;
import java.util.*;
import java.lang.ref.*;
import java.security.*;
import java.rmi.*;
import java.rmi.server.*;
import javax.transaction.xa.*;

public class ConnectionImpl extends UnicastRemoteObject implements Connection {
	StoreImpl store;
	Transaction transaction;
	Map cache=new WeakHashMap();
	Map refCache=new WeakHashMap();
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
		store.connections.add(this);
	}

	public Remote create(String name) {
		try {
			return create(Class.forName(name));
		} catch (ClassNotFoundException e) {
			throw new PersistentException("class not found");
		}
	}

	public Remote create(Class clazz) {
		return create(clazz, new Class[] {}, new Object[] {});
	}

	public Remote create(Class clazz, Class types[], Object args[]) {
		return create(new PersistentClass(clazz),types,args);
	}

	public RemoteArray create(Class componentType, int length) {
		return (RemoteArray)create(new ArrayClass(componentType,length), new Class[] {}, new Object[] {});
	}

	public RemoteArray create(Object component[]) {
		Class componentType=component.getClass().getComponentType();
		int length=component.length;
		return (RemoteArray)create(new ArrayClass(componentType,length), new Class[] {Object[].class}, new Object[] {component});
	}

	synchronized PersistentObject create(PersistentClass c, Class types[], Object args[]) {
		if(closed) throw new PersistentException("connection closed");
		return cache(c.newInstance(store.create(c),this,types,args));
	}

	PersistentObject cache(PersistentObject obj) {
		cache.put(obj.base,new WeakReference(obj));
		refCache.put(obj.peer,new WeakReference(obj));
		return obj;
	}

	PersistentObject instantiate(Accessor a) {
		return cache(a);
	}

	synchronized PersistentObject cache(Accessor a) {
		PersistentObject obj;
		Reference w;
		if((obj=(w=(Reference)cache.get(new Long(a.base)))==null?null:(PersistentObject)w.get())==null) {
			obj=a.clazz.newInstance(a,this);
			cache.put(obj.base,new WeakReference(obj));
			refCache.put(obj.peer,new WeakReference(obj));
		}
		return obj;
	}

	Object attach(Object obj) {
		return obj instanceof Accessor?instantiate((Accessor)obj):obj;
	}

	Object detach(Object obj) {
		if(obj instanceof PersistentObject) {
			PersistentObject b;
			if((b=(PersistentObject)obj).connection==this) return b.accessor;
			else throw new PersistentException("not the same connection");
		} else if(obj instanceof RemoteStub) {
			Reference w;
			return (w=(Reference)refCache.get(obj))==null?obj:((PersistentObject)w.get()).accessor;
		} else return obj;
	}

	public Object getRoot() {
		if(closed) throw new PersistentException("connection closed");
		return ((PersistentSystem)attach(store.system.accessor)).getRoot();
	}

	public void setRoot(Object obj) {
		if(closed) throw new PersistentException("connection closed");
		((PersistentSystem)attach(store.system.accessor)).setRoot(obj);
	}

	public int getTransactionIsolation() {
		if(closed) throw new PersistentException("connection closed");
		return level;
	}

	public void setTransactionIsolation(int level) {
		if(closed) throw new PersistentException("connection closed");
		if(transaction==null || this.level<level) this.level=level;
		if(transaction!=null) transaction.setLevel(this.level);
	}

	public void setAutoCommit(boolean autoCommit) {
		if(closed) throw new PersistentException("connection closed");
		this.autoCommit=autoCommit;
	}

	public boolean isReadOnly() {
		if(closed) throw new PersistentException("connection closed");
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		if(closed) throw new PersistentException("connection closed");
		if(transaction==null || readOnly) this.readOnly=readOnly;
		if(transaction!=null) transaction.setReadOnly(this.readOnly);
	}

	synchronized Accessor copy(Accessor obj, boolean read) {
		if(closed) throw new PersistentException("connection closed");
		if(!read && this.readOnly) throw new PersistentException("read only");
		if(transaction==null && level!=TRANSACTION_NONE) transaction=store.getTransaction(level,readOnly,name);
		return transaction==null?obj:transaction.copy(obj,read);
	}

	void autoCommit() {
		if(autoCommit) commit();
	}

	public synchronized void commit() {
		if(closed) throw new PersistentException("connection closed");
		if(transaction!=null) {
			transaction.commit();
			transaction=null;
		}
	}

	public synchronized void rollback() {
		if(closed) throw new PersistentException("connection closed");
		if(transaction!=null) {
			transaction.rollback();
			transaction=null;
		}
	}

	public synchronized void close() throws RemoteException {
		if(closed) throw new PersistentException("connection closed");
		Iterator t=cache.values().iterator();
		while(t.hasNext()) {
			PersistentObject obj;
			Reference w;
			if((obj=(w=(Reference)t.next())==null?null:(PersistentObject)w.get())==null);
			else UnicastRemoteObject.unexportObject(obj,true);
		}
		rollback();
		UnicastRemoteObject.unexportObject(this,true);
		store.connections.remove(this);
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
			throw new RuntimeException("remote error");
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
			throw new RuntimeException("remote error");
		}
	}

	public boolean setTransactionTimeout(int seconds) {
		return seconds==0;
	}

	public void start(Xid xid, int flags) {}
}
