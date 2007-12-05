package persistence;

public class MethodCall {
	PersistentObject target;
	String method;
	Class types[];
	Object args[];

	public MethodCall(PersistentObject target, String method, Class types[], Object args[]) {
		this.target=target;
		this.method=method;
		this.types=types;
		this.args=args;
	}

	PersistentObject target() {
		return ((Accessor)target.accessor).object;
	}

	Object[] args() {
		return Accessor.attach(Accessor.detach(args));
	}

	Object execute() {
		return execute(target());
	}

	Object execute(PersistentObject target) {
		return target.call(method,types,args());
	}
}
