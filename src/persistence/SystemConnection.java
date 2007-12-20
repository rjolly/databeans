package persistence;

import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import persistence.PersistentObject.MethodCall;

class SystemConnection extends Connection {
	SystemConnection(StoreImpl store, Subject subject) throws RemoteException {
		connection=new RemoteSystemConnection(store,subject);
	}

	class RemoteSystemConnection extends RemoteConnectionImpl {
		RemoteSystemConnection(StoreImpl store, Subject subject) throws RemoteException {
			super(store,Connection.TRANSACTION_NONE,false,subject);
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

		Object execute(final MethodCall call, MethodCall undo, int index, boolean read) {
			return Subject.doAsPrivileged(subject,new PrivilegedAction() {
				public Object run() {
					return call.execute();
				}
			},null);
		}

		public void commit() {}

		public void rollback() {}
	}

	PersistentObject attach(PersistentObject obj) {
		return obj;
	}
}
