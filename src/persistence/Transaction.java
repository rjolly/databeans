package persistence;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentCollections;
import persistence.util.RemoteCollection;

public class Transaction extends PersistentObject implements RemoteTransaction {
	public Transaction() throws RemoteException {}

	public Transaction(Accessor accessor, Connection connection, String client, RemoteCollection transactions) throws RemoteException {
		super(accessor,connection);
		setClient(client);
		setTransactions(transactions);
		setMethodCalls((RemoteCollection)create(PersistentArrayList.class));
		setObjects((RemoteCollection)create(PersistentArrayList.class));
		PersistentCollections.localCollection(transactions).add(this);
	}

	void lock(PersistentObject target) {
		Collection o=PersistentCollections.localCollection(getObjects());
		if(!o.contains(target)) {
			target.lock(this);
			o.add(target);
		}
	}

	void record(PersistentObject target, String method, Class types[], Object args[]) {
		Collection m=PersistentCollections.localCollection(getMethodCalls());
		m.add(create(MethodCall.class,new Class[] {PersistentObject.class, String.class, Class[].class, Object[].class},new Object[] {target, method, types, args}));
	}

	void commit() {
		unlock();
		close();
	}

	void rollback() {
		Collection m=PersistentCollections.localCollection(getMethodCalls());
		for(Iterator it=m.iterator();it.hasNext();) ((MethodCall)it.next()).execute();
		unlock();
		close();
	}

	void unlock() {
		Collection o=PersistentCollections.localCollection(getObjects());
		for(Iterator it=o.iterator();it.hasNext();) ((PersistentObject)it.next()).unlock();
	}

	void kick() {
		Collection o=PersistentCollections.localCollection(getObjects());
		for(Iterator it=o.iterator();it.hasNext();) ((PersistentObject)it.next()).kick();
	}

	void close() {
		PersistentCollections.localCollection(getTransactions()).remove(this);
	}

	public String getClient() {
		return (String)get("client");
	}

	public void setClient(String str) {
		set("client",str);
	}

	public RemoteCollection getTransactions() {
		return (RemoteCollection)get("transactions");
	}

	public void setTransactions(RemoteCollection collection) {
		set("transactions",collection);
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
