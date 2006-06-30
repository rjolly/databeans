package persistence;

import java.util.*;
import java.rmi.*;
import persistence.util.*;
import persistence.storage.*;

public class Transaction extends PersistentObject implements RemoteTransaction {
	public Transaction() throws RemoteException {}

	public Transaction(Accessor accessor, Connection connection, Integer level, Boolean readOnly, String client) throws RemoteException {
		super(accessor,connection);
		setLevel(level.intValue());
		setReadOnly(readOnly.booleanValue());
		setClient(client);
		setDuplicates((PersistentHashMap)create(PersistentHashMap.class));
		this.connection.store.transactions.add(this);
	}

	Accessor copy(Accessor obj, boolean read) {
		switch(getLevel()) {
		case Connection.TRANSACTION_READ_UNCOMMITTED:
			return read || isReadOnly()?obj:backup(obj);
		case Connection.TRANSACTION_READ_COMMITTED:
			return read || isReadOnly()?obj:copy(obj);
		case Connection.TRANSACTION_REPEATABLE_READ:
			return isReadOnly()?obj:copy(obj);
		case Connection.TRANSACTION_SERIALIZABLE:
			if(isReadOnly()) return obj;
			else {
				obj.lock(this);
				return copy(obj);
			}
		default:
			throw new PersistentException("bad transaction isolation level");
		}
	}

	Accessor copy(Accessor obj) {
		Map map=PersistentCollections.localMap(getDuplicates());
		Array duplicate;
		Number base=MemoryModel.model.toNumber(obj.base);
		if(map.containsKey(base)) duplicate=PersistentArrays.localArray((RemoteArray)map.get(base));
		else {
			duplicate=PersistentArrays.localArray(create(new Object[] {connection.attach(obj),connection.attach((Accessor)obj.clone())}));
			map.put(base,PersistentArrays.remoteArray(duplicate));
		}
		return ((PersistentObject)duplicate.get(1)).accessor;
	}

	Accessor backup(Accessor obj) {
		Map map=PersistentCollections.localMap(getDuplicates());
		Array duplicate;
		Number base=MemoryModel.model.toNumber(obj.base);
		if(map.containsKey(base));
		else {
			duplicate=PersistentArrays.localArray(create(new Object[] {connection.attach(obj),connection.attach((Accessor)obj.clone())}));
			map.put(base,PersistentArrays.remoteArray(duplicate));
			((PersistentObject)duplicate.get(1)).accessor.setLock(this);
		}
		return obj;
	}

	void commit() {
		Iterator t=PersistentCollections.localMap(getDuplicates()).values().iterator();
		while(t.hasNext()) {
			Array duplicate=PersistentArrays.localArray((RemoteArray)t.next());
			Accessor obj=((PersistentObject)duplicate.get(0)).accessor;
			Accessor copy=((PersistentObject)duplicate.get(1)).accessor;
			if(copy.getLock()==null) copy.copyInto(obj);
			if(obj.getLock()!=null) obj.unlock();
		}
		close();
	}

	void rollback() {
		Iterator t=PersistentCollections.localMap(getDuplicates()).values().iterator();
		while(t.hasNext()) {
			Array duplicate=PersistentArrays.localArray((RemoteArray)t.next());
			Accessor obj=((PersistentObject)duplicate.get(0)).accessor;
			Accessor copy=((PersistentObject)duplicate.get(1)).accessor;
			if(copy.getLock()!=null) copy.copyInto(obj);
			if(obj.getLock()!=null) obj.unlock();
		}
		close();
	}

	void close() {
		connection.store.transactions.remove(this);
	}

	public int getLevel() {
		return ((Integer)get("level")).intValue();
	}

	public void setLevel(int n) {
		set("level",new Integer(n));
	}

	public boolean isReadOnly() {
		return ((Boolean)get("readOnly")).booleanValue();
	}

	public void setReadOnly(boolean b) {
		set("readOnly",new Boolean(b));
	}

	public String getClient() {
		return (String)get("client");
	}

	public void setClient(String str) {
		set("client",str);
	}

	public RemoteMap getDuplicates() {
		return (RemoteMap)get("duplicates");
	}

	public void setDuplicates(RemoteMap map) {
		set("duplicates",map);
	}

	public String remoteToString() {
		return getClient();
	}
}
