package persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

public class PersistentMethodCall extends PersistentObject implements RemoteMethodCall {
	public PersistentMethodCall() throws RemoteException {}

	public PersistentMethodCall(Accessor accessor, Connection connection, MethodCall call) throws RemoteException {
		super(accessor,connection);
		setTarget(call.target);
		setMethod(call.method);
		setTypes(create(call.types));
		setArgs(create(call.args));
	}

	Object execute() {
		Array t=PersistentArrays.localArray(getTypes());
		Array a=PersistentArrays.localArray(getArgs());
		Class types[]=new Class[t.length()];
		Object args[]=new Object[a.length()];
		PersistentArrays.copy(t,0,types,0,types.length);
		PersistentArrays.copy(a,0,args,0,args.length);
		return new MethodCall((PersistentObject)getTarget(),getMethod(),types,args).execute();
	}

	public Remote getTarget() {
		return (Remote)get("target");
	}

	public void setTarget(Remote obj) {
		set("target",obj);
	}

	public String getMethod() {
		return (String)get("method");
	}

	public void setMethod(String str) {
		set("method",str);
	}

	public RemoteArray getTypes() {
		return (RemoteArray)get("types");
	}

	public void setTypes(RemoteArray array) {
		set("types",array);
	}

	public RemoteArray getArgs() {
		return (RemoteArray)get("args");
	}

	public void setArgs(RemoteArray array) {
		set("args",array);
	}

	public String remoteToString() {
		return getMethod();
	}
}
