package persistence;

import java.rmi.RemoteException;
import java.security.Principal;

class SystemConnection extends ConnectionImpl {
	SystemConnection(StoreImpl store, Principal user) throws RemoteException {
		super(store,TRANSACTION_NONE,user);
	}

	void open() {}

	Object execute(MethodCall call) {
		return execute(call,null,0,true);
	}

	Object execute(MethodCall call, MethodCall undo, int index) {
		return execute(call,undo,index,false);
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(closed) throw new PersistentException("connection closed");
		if(!read && readOnly) throw new PersistentException("read only");
		return call.execute();
	}

	synchronized void commit() {
		if(closed) throw new PersistentException("connection closed");
	}

	synchronized void rollback() {
		if(closed) throw new PersistentException("connection closed");
	}

	synchronized void close(boolean force) {
		if(closed) throw new PersistentException("connection closed");
		closed=true;
	}
}
