package persistence;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import persistence.PersistentObject.MethodCall;

public class Connection implements Serializable {
	static final int TRANSACTION_NONE = 0;
	static final int TRANSACTION_READ_UNCOMMITTED = 1;
	static final int TRANSACTION_READ_COMMITTED = 2;
	static final int TRANSACTION_REPEATABLE_READ = 3;
	static final int TRANSACTION_SERIALIZABLE = 4;

	persistence.RemoteConnection connection;
	transient Map cache;

	Connection() {}

	Connection(StoreImpl store, int level, Subject subject) throws RemoteException {
		connection=new RemoteConnection(store,level,subject);
	}

	class RemoteConnection extends RemoteConnectionImpl {
		RemoteConnection(StoreImpl store, int level, Subject subject) throws RemoteException {
			super(store,level,false,subject);
		}

		Connection connection() {
			return Connection.this;
		}
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

	PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
		try {
			return attach(connection.create(clazz,types,args));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public PersistentSystem getSystem() {
		try {
			return (PersistentSystem)attach(connection.getSystem());
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object root() {
		return getSystem().root();
	}

	public void setRoot(Object obj) {
		getSystem().setRoot(obj);
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

	Object attach(Object obj) {
		return obj instanceof PersistentObject?attach((PersistentObject)obj):obj;
	}

	PersistentObject attach(PersistentObject obj) {
		if(cache==null) cache=new WeakHashMap();
		synchronized(cache) {
			Accessor accessor=obj.accessor;
			PersistentObject b=get(accessor);
			if(b==null) {
				obj.connection=this;
				cache.put(accessor,new WeakReference(obj));
			} else obj=b;
			return obj;
		}
	}

	PersistentObject get(Accessor accessor) {
		Reference w=(Reference)cache.get(accessor);
		return w==null?null:(PersistentObject)w.get();
	}

	public Object execute(MethodCall call) {
		try {
			return attach(connection.execute(call));
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public Object execute(MethodCall call, MethodCall undo, int index) {
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
		if(cache==null) return;
		while(true) {
			try {
				for(Iterator it=cache.keySet().iterator();it.hasNext();it.remove()) {
					PersistentObject obj=get((Accessor)it.next());
					if(obj!=null) obj.close();
				}
				break;
			} catch (ConcurrentModificationException e) {}
		}
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
