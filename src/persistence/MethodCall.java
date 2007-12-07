package persistence;

import java.io.Serializable;

public class MethodCall implements Serializable {
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

	MethodCall attach(StoreImpl store) {
		return new MethodCall(attach(store,target),method,types,attach(store,args));
	}

	static Object attach(Connection connection, Object obj) {
		return obj instanceof PersistentObject?((PersistentObject)obj).attach(connection):obj;
	}

	static Object attach(StoreImpl store, Object obj) {
		return obj instanceof PersistentObject?attach(store,(PersistentObject)obj):obj;
	}

	static PersistentObject attach(StoreImpl store, PersistentObject obj) {
		return obj.attach(store);
	}

	static Object[] attach(StoreImpl store, Object obj[]) {
		Object a[]=new Object[obj.length];
		for(int i=0;i<obj.length;i++) a[i]=attach(store,obj[i]);
		return a;
	}

	Object execute() {
		return execute(target);
	}

	Object execute(PersistentObject target) {
		return target.call(method,types,args);
	}

	static Object execute(PersistentMethodCall call) {
		Array t=call.getTypes();
		Array a=call.getArgs();
		Class types[]=new Class[t.length()];
		Object args[]=new Object[a.length()];
		Arrays.copy(t,0,types,0,types.length);
		Arrays.copy(a,0,args,0,args.length);
		return new MethodCall(call.getTarget(),call.getMethod(),types,args).execute();
	}

	public String toString() {
		return method;
	}
}
