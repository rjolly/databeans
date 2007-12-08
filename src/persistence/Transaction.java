package persistence;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import persistence.PersistentObject.MethodCall;
import persistence.storage.MemoryModel;
import persistence.util.PersistentArrayList;
import persistence.util.PersistentHashMap;

public class Transaction extends PersistentObject {
	public void init(String client) {
		setClient(client);
		setCalls((List)create(PersistentArrayList.class));
		setUndos((List)create(PersistentArrayList.class));
		setPairs((Map)create(PersistentHashMap.class));
	}

	PersistentObject copy(PersistentObject obj, int level, boolean read, boolean readOnly) {
		switch(level) {
		case Connection.TRANSACTION_READ_UNCOMMITTED:
			return obj;
		case Connection.TRANSACTION_READ_COMMITTED:
			return read?obj:copy(obj);
		case Connection.TRANSACTION_REPEATABLE_READ:
			return copy(obj);
		case Connection.TRANSACTION_SERIALIZABLE:
			if(!readOnly) obj.lock(this);
			return copy(obj);
		default:
			throw new PersistentException("bad transaction isolation level");
		}
	}

	PersistentObject copy(PersistentObject obj) {
		Array pair;
		Map map=getPairs();
//		Long base=obj.base();
		Number base=MemoryModel.model.toNumber(obj.base().longValue());
		if(map.containsKey(base)) pair=(Array)map.get(base);
		else {
			pair=(Array)create(new Object[] {obj,obj.clone()});
			map.put(base,pair);
		}
		return (PersistentObject)pair.get(1);
	}

	void record(MethodCall call, MethodCall undo, int level) {
		List calls=getCalls();
		List undos=getUndos();
		switch(level) {
		case Connection.TRANSACTION_READ_UNCOMMITTED:
			undos.add(call(undo));
			break;
		case Connection.TRANSACTION_READ_COMMITTED:
		case Connection.TRANSACTION_REPEATABLE_READ:
		case Connection.TRANSACTION_SERIALIZABLE:
			calls.add(call(call));
			break;
		default:
			throw new PersistentException("bad transaction isolation level");
		}
	}

	PersistentMethodCall call(MethodCall call) {
		return (PersistentMethodCall)create(PersistentMethodCall.class,new Class[] {MethodCall.class},new Object[] {call});
	}

	void commit() {
		List l=getCalls();
		for(ListIterator it=l.listIterator(0);it.hasNext();it.remove()) {
			PersistentObject.execute(((PersistentMethodCall)it.next()));
		}
		getUndos().clear();
		unlock();
	}

	void rollback() {
		List l=getUndos();
		for(ListIterator it=l.listIterator(l.size());it.hasPrevious();it.remove()) {
			PersistentObject.execute((PersistentMethodCall)it.previous());
		}
		getCalls().clear();
		unlock();
	}

	void unlock() {
		Collection o=getPairs().values();
		for(Iterator it=o.iterator();it.hasNext();it.remove()) {
			((PersistentObject)((Array)it.next()).get(0)).unlock();
		}
	}

	void kick() {
		Collection o=getPairs().values();
		for(Iterator it=o.iterator();it.hasNext();) {
			((PersistentObject)((Array)it.next()).get(0)).kick();
		}
	}

	public String getClient() {
		return (String)get("client");
	}

	public void setClient(String str) {
		set("client",str);
	}

	public List getCalls() {
		return (List)get("calls");
	}

	public void setCalls(List list) {
		set("calls",list);
	}

	public List getUndos() {
		return (List)get("undos");
	}

	public void setUndos(List list) {
		set("undos",list);
	}

	public Map getPairs() {
		return (Map)get("pairs");
	}

	public void setPairs(Map map) {
		set("pairs",map);
	}

	public String toString() {
		return getClient();
	}
}
