package persistence;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.security.auth.Subject;
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

	Object execute(MethodCall call, MethodCall undo, int index, int level, boolean read, boolean readOnly, Subject subject) {
		PersistentObject target=copy(call.target(),level,read,readOnly);
		Object obj=call.execute(target,subject);
		if(!read) {
			undo.args[index]=obj;
			record(call,undo,level);
		}
		return obj;
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

	synchronized PersistentObject copy(PersistentObject obj) {
		if(isRollbackOnly()) throw new PersistentException("rollback only");
		Array pair;
		Map map=getPairs();
//		Long base=new Long(obj.accessor().base());
		Number base=MemoryModel.model.toNumber(obj.accessor().base());
		if(map.containsKey(base)) pair=(Array)map.get(base);
		else {
			pair=(Array)create(new Object[] {obj,obj.clone()});
			map.put(base,pair);
		}
		return (PersistentObject)pair.get(1);
	}

	void record(MethodCall call, MethodCall undo, int level) {
		switch(level) {
		case Connection.TRANSACTION_READ_UNCOMMITTED:
			getUndos().add(call(undo));
			break;
		case Connection.TRANSACTION_READ_COMMITTED:
		case Connection.TRANSACTION_REPEATABLE_READ:
		case Connection.TRANSACTION_SERIALIZABLE:
			getCalls().add(call(call));
			break;
		default:
			throw new PersistentException("bad transaction isolation level");
		}
	}

	PersistentMethodCall call(MethodCall call) {
		return (PersistentMethodCall)create(PersistentMethodCall.class,new Class[] {MethodCall.class},new Object[] {call});
	}

	synchronized void commit(Subject subject) {
		if(isRollbackOnly()) throw new PersistentException("rollback only");
		for(ListIterator it=getCalls().listIterator(0);it.hasNext();it.remove()) {
			((PersistentMethodCall)it.next()).call().execute(subject);
		}
		getUndos().clear();
		clear();
	}

	synchronized void rollback(Subject subject) {
		for(ListIterator it=getUndos().listIterator(getUndos().size());it.hasPrevious();it.remove()) {
			((PersistentMethodCall)it.previous()).call().execute(subject);
		}
		getCalls().clear();
		clear();
		setRollbackOnly(false);
	}

	synchronized void unlock() {
		clear();
		setRollbackOnly(true);
	}

	void clear() {
		for(Iterator it=getPairs().values().iterator();it.hasNext();it.remove()) {
			((PersistentObject)((Array)it.next()).get(0)).unlock();
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

	public boolean isRollbackOnly() {
		return ((Boolean)get("rollbackOnly")).booleanValue();
	}

	public void setRollbackOnly(boolean b) {
		set("rollbackOnly",new Boolean(b));
	}

	public String toString() {
		return "["+getClient()+", "+getCalls()+", "+getUndos()+", "+getPairs()+", "+isRollbackOnly()+"]";
	}
}
