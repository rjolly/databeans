package persistence;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import persistence.util.PersistentArrayList;

public class Transaction extends PersistentObject {
	protected void init(String client) {
		setClient(client);
		setMethodCalls((List)create(PersistentArrayList.class));
		setObjects((Collection)create(PersistentArrayList.class));
	}

	void lock(PersistentObject target) {
		Collection o=getObjects();
		if(!o.contains(target)) {
			target.lock(this);
			o.add(target);
		}
	}

	void record(MethodCall call) {
		getMethodCalls().add(create(PersistentMethodCall.class,new Class[] {MethodCall.class},new Object[] {call}));
	}

	void commit() {
		unlock();
	}

	void rollback() {
		List l=getMethodCalls();
		for(ListIterator it=l.listIterator(l.size());it.hasPrevious();it.remove()) {
			((PersistentMethodCall)it.previous()).execute();
		}
		unlock();
	}

	void unlock() {
		Collection o=getObjects();
		for(Iterator it=o.iterator();it.hasNext();it.remove()) {
			((PersistentObject)it.next()).unlock();
		}
	}

	void kick() {
		Collection o=getObjects();
		for(Iterator it=o.iterator();it.hasNext();) {
			((PersistentObject)it.next()).kick();
		}
	}

	public String getClient() {
		return (String)get("client");
	}

	public void setClient(String str) {
		set("client",str);
	}

	public List getMethodCalls() {
		return (List)get("methodCalls");
	}

	public void setMethodCalls(List list) {
		set("methodCalls",list);
	}

	public Collection getObjects() {
		return (Collection)get("objects");
	}

	public void setObjects(Collection collection) {
		set("objects",collection);
	}

	public String toString() {
		return getClient();
	}
}
