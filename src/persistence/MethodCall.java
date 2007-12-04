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

	Object execute() {
		return ((Accessor)target.accessor).call(method,types,attach(detach(args)));
	}

	static Object[] attach(Object obj[]) {
		Object a[]=new Object[obj.length];
		for(int i=0;i<obj.length;i++) a[i]=Accessor.attach(obj[i]);
		return a;
	}

	static Object[] detach(Object obj[]) {
		Object a[]=new Object[obj.length];
		for(int i=0;i<obj.length;i++) a[i]=Accessor.detach(obj[i]);
		return a;
	}
}
