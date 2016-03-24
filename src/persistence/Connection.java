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

	Connection() {}

	static Connection newInstance(Store store, int level, Subject subject) throws RemoteException {
		Connection conn=new Connection();
		conn.connection=new RemoteConnection(conn,store,level,false,subject);
		return conn;
	}

	static AdminConnection newInstance(Store store, boolean readOnly, Subject subject) throws RemoteException {
		AdminConnection conn=new AdminConnection();
		((Connection)conn).connection=new RemoteAdminConnection(conn,store,readOnly,subject);
		conn.connection=(RemoteAdminConnection)((Connection)conn).connection;
		return conn;
	}

	static SystemConnection newInstance(Store store) throws RemoteException {
		SystemConnection conn=new SystemConnection();
		conn.connection=new RemoteSystemConnection(conn,store);
		return conn;
	}

	public Subject subject() {
		return subject==null?subject=connection.subject():subject;
	}

	public int getTransactionIsolation() {
		return connection.getTransactionIsolation();
	}

	public void setTransactionIsolation(int level) {
		connection.setTransactionIsolation(level);
	}

	public boolean isReadOnly() {
		return connection.isReadOnly();
	}

	public void setReadOnly(boolean readOnly) {
		connection.setReadOnly(readOnly);
	}

	public boolean isAutoCommit() {
		return connection.isAutoCommit();
	}

	public void setAutoCommit(boolean autoCommit) {
		connection.setAutoCommit(autoCommit);
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

	void cache(PersistentObject obj) {
		cache.remove(obj.accessor);
		cache.put(obj.accessor,new WeakReference(obj));
	}

	PersistentObject get(Accessor accessor) {
		Reference w=(Reference)cache.get(accessor);
		return w==null?null:(PersistentObject)w.get();
	}

	PersistentClass getClass(Accessor accessor) {
		try {
			return (PersistentClass)attach(accessor.clazz());
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
	}

	public boolean setTransactionTimeout(int seconds) {
		return seconds==0;
	}

	public void start(Xid xid, int flags) {}
}
