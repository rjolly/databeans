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
	}

	void lock(PersistentObject target) {
		Collection o=PersistentCollections.localCollection(getObjects());
		if(!o.contains(target)) {
			target.lock(this);
			o.add(target);
		}
	}

	void record(MethodCall call) {
		PersistentCollections.localCollection(getMethodCalls()).add(call);
	}

	void commit() {
		unlock();
	}

	void rollback() {
		for(Iterator it=PersistentCollections.localCollection(getMethodCalls()).iterator();it.hasNext();it.remove()) ((MethodCall)it.next()).execute();
		unlock();
	}

	void unlock() {
		for(Iterator it=PersistentCollections.localCollection(getObjects()).iterator();it.hasNext();it.remove()) ((PersistentObject)it.next()).unlock();
	}

	void kick() {
		for(Iterator it=PersistentCollections.localCollection(getObjects()).iterator();it.hasNext();) ((PersistentObject)it.next()).kick();
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
