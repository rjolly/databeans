package persistence;

import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import persistence.PersistentObject.MethodCall;
import persistence.server.DatabeansPrincipal;

class SystemConnection extends Connection {
	static Subject systemSubject;
	static {
		Set s=new HashSet();
		s.add(new DatabeansPrincipal("system"));
		systemSubject=new Subject(true,s,new HashSet(),new HashSet());
	}
	
	SystemConnection(StoreImpl store) throws RemoteException {
		super(store,TRANSACTION_NONE,systemSubject);
	}

	public Object execute(MethodCall call) {
		return execute(call,null,0,true);
	}

	public Object execute(MethodCall call, MethodCall undo, int index) {
		return execute(call,undo,index,false);
	}

	Object execute(final MethodCall call, MethodCall undo, int index, boolean read) {
		return Subject.doAsPrivileged(systemSubject,new PrivilegedAction() {
			public Object run() {
				return call.execute();
			}
		},null);
	}

	public void commit() {}

	public void rollback() {}
}
