package persistence;

import java.rmi.RemoteException;
import persistence.PersistentObject.MethodCall;

class SystemConnection extends Connection {
	SystemConnection() {}

	PersistentObject attach(PersistentObject obj) {
		return obj;
	}

	public void close() {
		connection=null;
	}
}

class RemoteSystemConnection extends RemoteConnection {
	RemoteSystemConnection(SystemConnection connection, Store store) throws RemoteException {
		super(connection,store,false,null);
	}

	void open() {}

	public PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
		return create(clazz,types,args,true);
	}

	public Object execute(MethodCall call) {
		return call.execute();
	}

	public Object executeAtomic(MethodCall call) {
		return execute(call,null,0,true);
	}

	public Object executeAtomic(MethodCall call, MethodCall undo, int index) {
		return execute(call,undo,index,false);
	}

	Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
		return call.execute(subject);
	}

	public void commit() {}

	public void rollback() {}
}
