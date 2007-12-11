package persistence;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import persistence.PersistentObject.MethodCall;

class SystemConnection extends Connection {
	SystemConnection(StoreImpl store) throws RemoteException {
		super(new SystemConnectionImpl(store));
	}
}

class SystemConnectionImpl extends RemoteConnectionImpl {
	SystemConnectionImpl(StoreImpl store) throws RemoteException {
		super(store,Transaction.TRANSACTION_NONE,null);
	}

	void open() {}

	public Object execute(MethodCall call) {
		return execute(call,null,0,true);
	}

	public Object execute(MethodCall call, MethodCall undo, int index) {
		return execute(call,undo,index,false);
	}

	synchronized Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		if(!read && readOnly) throw new PersistentException("read only");
		return call.execute();
	}

	public void commit() {}

	public void rollback() {}

	public synchronized void close(boolean force) throws RemoteException {
		UnicastRemoteObject.unexportObject(this,true);
	}
}
