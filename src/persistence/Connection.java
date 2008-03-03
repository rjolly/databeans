package persistence;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import persistence.PersistentObject.MethodCall;

public class Connection implements Serializable {
	public static final int TRANSACTION_NONE = 0;
	public static final int TRANSACTION_READ_UNCOMMITTED = 1;
	public static final int TRANSACTION_READ_COMMITTED = 2;
	public static final int TRANSACTION_REPEATABLE_READ = 3;
	public static final int TRANSACTION_SERIALIZABLE = 4;

	RemoteConnection connection;
	transient Subject subject;
	transient Map cache;

	Connection(RemoteConnection connection) {
		this.connection=connection;
	}

	Connection(StoreImpl store, int level, Subject subject) throws RemoteException {
		this(new RemoteConnectionImpl(store,level,false,subject));
	}

	public PersistentObject create(String name) {
		return create(get(name),new Class[] {},new Object[] {});
	}

	public PersistentObject create(Class clazz) {
		return create(get(clazz),new Class[] {},new Object[] {});
	}

	public PersistentObject create(Class clazz, Class types[], Object args[]) {
		return create(get(clazz),types,args);
	}

	public PersistentArray create(Class componentType, int length) {
		return (PersistentArray)create(get(componentType,length),new Class[] {},new Object[] {});
	}

	public PersistentArray create(Object component[]) {
		Class componentType=component.getClass().getComponentType();
		int length=component.length;
		return (PersistentArray)create(get(componentType,length),new Class[] {Object[].class},new Object[] {component});
	}

	PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
		try {
			return attach(connection.create(clazz,types,args));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	PersistentClass get(String name) {
		try {
			return (PersistentClass)attach(connection.get(name));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public PersistentClass get(Class clazz) {
		try {
			return (PersistentClass)attach(connection.get(clazz));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	PersistentClass get(Class componentType, int length) {
		try {
			return (PersistentClass)attach(connection.get(componentType,length));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public PersistentSystem system() {
		try {
			return (PersistentSystem)attach(connection.system());
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object root() {
		return system().root();
	}

	public void setRoot(Object obj) {
		system().setRoot(obj);
	}

	public Subject subject() {
		try {
			return subject==null?subject=connection.subject():subject;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public int getTransactionIsolation() {
		try {
			return connection.getTransactionIsolation();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void setTransactionIsolation(int level) {
		try {
			connection.setTransactionIsolation(level);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isReadOnly() {
		try {
			return connection.isReadOnly();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void setReadOnly(boolean readOnly) {
		try {
			connection.setReadOnly(readOnly);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isAutoCommit() {
		try {
			return connection.isAutoCommit();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void setAutoCommit(boolean autoCommit) {
		try {
			connection.setAutoCommit(autoCommit);
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	Map cache() {
		return cache==null?cache=new WeakHashMap():cache;
	}

	Object attach(Object obj) {
		if(obj instanceof PersistentObject) return attach((PersistentObject)obj);
		if(obj instanceof Object[]) return attach((Object[])obj);
		return obj;
	}

	Object[] attach(Object obj[]) {
		for(int i=0;i<obj.length;i++) obj[i]=attach(obj[i]);
		return obj;
	}

	PersistentObject attach(PersistentObject obj) {
		cache();
		synchronized(cache) {
			PersistentObject o=get(obj.accessor);
			return o==null?cache(obj):o;
		}
	}

	PersistentObject cache(PersistentObject obj) {
		obj.connection=this;
		cache.put(obj.accessor,new WeakReference(obj));
		return obj;
	}

	PersistentObject get(Accessor accessor) {
		Reference w=(Reference)cache.get(accessor);
		PersistentObject o=w==null?null:(PersistentObject)w.get();
		if(o==null) cache.remove(accessor);
		return o;
	}

	Object execute(MethodCall call) {
		try {
			return attach(connection.execute(call));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	Object execute(MethodCall call, MethodCall undo, int index) {
		try {
			return attach(connection.execute(call,undo,index));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void commit() {
		try {
			connection.commit();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void rollback() {
		try {
			connection.rollback();
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		connection=null;
		cache();
		for(Iterator it=cache.keySet().iterator();it.hasNext();it.remove()) {
			close((Accessor)it.next());
		}
	}

	void close(Accessor accessor) {
		PersistentObject o;
		synchronized(cache) {
			o=get(accessor);
		}
		if(o!=null) o.close();
	}

	public boolean isClosed() {
		return connection==null;
	}

	public XAResource getXAResource() {
		return new ConnectionXAResource(this);
	}
}

class ConnectionXAResource implements XAResource {
	Connection connection;

	ConnectionXAResource(Connection connection) {
		this.connection=connection;
	}

	public void commit(Xid xid, boolean onePhase) {
		connection.commit();
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
		connection.rollback();
	}

	public boolean setTransactionTimeout(int seconds) {
		return seconds==0;
	}

	public void start(Xid xid, int flags) {}
}
