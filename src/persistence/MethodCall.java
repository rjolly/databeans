package persistence;

class MethodCall {
	Object target;
	String method;
	Class types[];
	Object args[];

	MethodCall(Object target, String method, Class types[], Object args[]) {
		this.target=target;
		this.method=method;
		this.types=types;
		this.args=args;
	}

	Object execute() {
		return ((PersistentObject)PersistentObject.remote(target)).call(method,types,args);
	}
}
