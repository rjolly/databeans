package persistence;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentCollections;
import persistence.util.RemoteCollection;

public class Transaction extends PersistentObject implements RemoteTransaction {
	public Transaction() throws RemoteException {}

	public Transaction(Accessor accessor, Connection connection, String client) throws RemoteException {
		super(accessor,connection);
		setClient(client);
		setMethodCalls((RemoteCollection)create(PersistentArrayList.class));
		setObjects((RemoteCollection)create(PersistentArrayList.class));
		this.connection.store.transactions.add(this);
	}

	void record(PersistentObject target) {
		Collection o=PersistentCollections.localCollection(getObjects());
		if(!o.contains(target)) o.add(target);
	}

	void record(PersistentObject target, String method, Class types[], Object args[]) {
		Collection o=PersistentCollections.localCollection(getObjects());
		Collection m=PersistentCollections.localCollection(getMethodCalls());
		m.add(create(MethodCall.class,new Class[] {PersistentObject.class, String.class, Class[].class, Object[].class},new Object[] {target, method, types, args}));
		if(!o.contains(target)) o.add(target);
	}

	void commit() {
		Collection o=PersistentCollections.localCollection(getObjects());
		for(Iterator it=o.iterator();it.hasNext();) {
			PersistentObject obj=(PersistentObject)it.next();
			obj.unlock();
		}
		close();
	}

	void rollback() {
		Collection o=PersistentCollections.localCollection(getObjects());
		Collection m=PersistentCollections.localCollection(getMethodCalls());
		for(Iterator it=m.iterator();it.hasNext();) {
			MethodCall call=(MethodCall)it.next();
			call.execute();
		}
		for(Iterator it=o.iterator();it.hasNext();) {
			PersistentObject obj=(PersistentObject)it.next();
			obj.unlock();
		}
		close();
	}

	void close() {
		connection.store.transactions.remove(this);
	}

	public String getClient() {
		return (String)get("client");
	}

	public void setClient(String str) {
		set("client",str);
	}

	public RemoteCollection getMethodCalls() {
		return (RemoteCollection)get("methodCalls");
	}

	public void setMethodCalls(RemoteCollection collection) {
		set("methodCalls",collection);
	}

	public RemoteCollection getObjects() {
		return (RemoteCollection)get("objects");
	}

	public void setObjects(RemoteCollection collection) {
		set("objects",collection);
	}

	public String remoteToString() {
		return getClient();
	}
}
