package persistence;

class MethodCall {
	PersistentObject target;
	String method;
	Class types[];
	Object args[];

	MethodCall(PersistentObject target, String method, Class types[], Object args[]) {
		this.target=target;
		this.method=method;
		this.types=types;
		this.args=args;
	}

	Object execute() {
		return target.call(method,types,args);
	}
}
