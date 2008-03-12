package persistence;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.security.auth.Subject;
import persistence.PersistentObject.MethodCall;
import persistence.storage.MemoryModel;
import persistence.util.ArrayList;
import persistence.util.LinkedHashMap;

public class Transaction extends PersistentObject {
	public void init(String client) {
		setClient(client);
		setCalls((List)create(ArrayList.class));
		setUndos((List)create(ArrayList.class));
		setPairs((Map)create(LinkedHashMap.class));
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		public String persistentToString() {
			return Long.toHexString(base)+"["+getClient()+"]"+(isRollbackOnly()?" (aborted)":"");
		}
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
			return copy(obj,read);
		case Connection.TRANSACTION_REPEATABLE_READ:
			return copy(obj,false);
		case Connection.TRANSACTION_SERIALIZABLE:
			if(!readOnly) obj.lock(this);
			return copy(obj,false);
		default:
			throw new PersistentException("bad transaction isolation level");
		}
	}

	synchronized PersistentObject copy(PersistentObject obj, boolean read) {
		if(isRollbackOnly()) {
			obj.unlock();
			throw new PersistentException("rollback only");
		}
		Array pair;
		Map map=getPairs();
//		Long base=new Long(obj.base);
		Number base=MemoryModel.model.toNumber(obj.base);
		if(map.containsKey(base)) pair=(Array)map.get(base);
		else {
			if(read) return obj;
			else {
				pair=(Array)create(new Object[] {obj,obj.clone()});
				map.put(base,pair);
			}
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

	synchronized void abort() {
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
}
