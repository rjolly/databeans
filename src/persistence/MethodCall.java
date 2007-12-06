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

	MethodCall detach() {
		return new MethodCall(target.accessor().object(),method,types,detach(args));
	}

	static Object attach(Connection connection, Object obj) {
		return obj instanceof PersistentObject?((PersistentObject)obj).accessor().object(connection):obj;
	}

	static Object detach(Object obj) {
		return obj instanceof PersistentObject?((PersistentObject)obj).accessor().object():obj;
	}

	static Object[] detach(Object obj[]) {
		Object a[]=new Object[obj.length];
		for(int i=0;i<obj.length;i++) a[i]=detach(obj[i]);
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
}
