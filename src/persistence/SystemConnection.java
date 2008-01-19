package persistence;

import java.rmi.RemoteException;
import persistence.PersistentObject.MethodCall;

class SystemConnection extends Connection {
	SystemConnection(StoreImpl store) throws RemoteException {
		connection=new RemoteSystemConnection(store);
	}

	class RemoteSystemConnection extends RemoteConnectionImpl {
		RemoteSystemConnection(StoreImpl store) throws RemoteException {
			super(store,Connection.TRANSACTION_NONE,false,null);
		}

		void open() {}

		Connection connection() {
			return SystemConnection.this;
		}

		public PersistentObject create(PersistentClass clazz, Class types[], Object args[]) {
			return create(clazz,types,args,true);
		}

		public Object execute(MethodCall call) {
			return execute(call,null,0,true);
		}

		public Object execute(MethodCall call, MethodCall undo, int index) {
			return execute(call,undo,index,false);
		}

		Object execute(MethodCall call, MethodCall undo, int index, boolean read) {
			return call.execute(subject);
		}

		public void commit() {}

		public void rollback() {}
	}

	PersistentObject attach(PersistentObject obj) {
		return obj;
	}

	public void close() {
		connection=null;
	}
}
