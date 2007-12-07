package persistence;

import java.rmi.RemoteException;
import javax.security.auth.Subject;

class SystemConnection extends ConnectionImpl {
	SystemConnection(StoreImpl store) throws RemoteException {
		super(store,TRANSACTION_NONE,new Subject());
	}

	void open() {}

	public Object execute(MethodCall call) {
		return execute(call,null,0,true);
	}

	public Object execute(MethodCall call, MethodCall undo, int index) {
		return execute(call,undo,index,false);
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(closed) throw new PersistentException("connection closed");
		if(!read && readOnly) throw new PersistentException("read only");
		return call.execute();
	}

	public synchronized void commit() {
		if(closed) throw new PersistentException("connection closed");
	}

	public synchronized void rollback() {
		if(closed) throw new PersistentException("connection closed");
	}

	synchronized void close(boolean force) {
		if(closed) throw new PersistentException("connection closed");
		closed=true;
	}
}
