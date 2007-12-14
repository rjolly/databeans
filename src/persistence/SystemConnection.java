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

		public Object execute(MethodCall call) {
			return execute(call,null,0,true);
		}

		public Object execute(MethodCall call, MethodCall undo, int index) {
			return execute(call,undo,index,false);
		}

		Object execute(final MethodCall call, final MethodCall undo, final int index, final boolean read) {
			return call.execute();
		}

		public void commit() {}

		public void rollback() {}

		Connection connection() {
			return SystemConnection.this;
		}
	}

	PersistentObject attach(PersistentObject obj) {
		return obj;
	}
}
