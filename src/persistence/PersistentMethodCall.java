package persistence;

public class PersistentMethodCall extends PersistentObject {
	protected void init(MethodCall call) {
		setTarget(call.target);
		setMethod(call.method);
		setTypes(create(call.types));
		setArgs(create(call.args));
	}

	Object execute() {
		Array t=getTypes();
		Array a=getArgs();
		Class types[]=new Class[t.length()];
		Object args[]=new Object[a.length()];
		Arrays.copy(t,0,types,0,types.length);
		Arrays.copy(a,0,args,0,args.length);
		return new MethodCall(getTarget(),getMethod(),types,args).execute();
	}

	public PersistentObject getTarget() {
		return (PersistentObject)get("target");
	}

	public void setTarget(PersistentObject obj) {
		set("target",obj);
	}

	public String getMethod() {
		return (String)get("method");
	}

	public void setMethod(String str) {
		set("method",str);
	}

	public Array getTypes() {
		return (Array)get("types");
	}

	public void setTypes(Array array) {
		set("types",array);
	}

	public Array getArgs() {
		return (Array)get("args");
	}

	public void setArgs(Array array) {
		set("args",array);
	}

	public String toString() {
		return getMethod();
	}
}
