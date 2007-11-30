package persistence;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentCollections;
import persistence.util.RemoteCollection;
import persistence.util.RemoteList;

public class Transaction extends PersistentObject implements RemoteTransaction {
	public Transaction() throws RemoteException {}

	public Transaction(Accessor accessor, Connection connection, String client) throws RemoteException {
		super(accessor,connection);
		setClient(client);
		setMethodCalls((RemoteList)create(PersistentArrayList.class));
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
		PersistentCollections.localList(getMethodCalls()).add(create(PersistentMethodCall.class,new Class[] {MethodCall.class},new Object[] {call}));
	}

	void commit() {
		unlock();
	}

	void rollback() {
		List l=PersistentCollections.localList(getMethodCalls());
		for(ListIterator it=l.listIterator(l.size());it.hasPrevious();it.remove()) ((PersistentMethodCall)it.previous()).execute();
		unlock();
	}

	void unlock() {
		Collection o=PersistentCollections.localCollection(getObjects());
		for(Iterator it=o.iterator();it.hasNext();it.remove()) ((PersistentObject)it.next()).unlock();
	}

	void kick() {
		Collection o=PersistentCollections.localCollection(getObjects());
		for(Iterator it=o.iterator();it.hasNext();) ((PersistentObject)it.next()).kick();
	}

	public String getClient() {
		return (String)get("client");
	}

	public void setClient(String str) {
		set("client",str);
	}

	public RemoteList getMethodCalls() {
		return (RemoteList)get("methodCalls");
	}

	public void setMethodCalls(RemoteList list) {
		set("methodCalls",list);
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
