package persistence.util;

import java.rmi.RemoteException;
import persistence.PersistentClass;
import persistence.PersistentObject;

public final class AbstractMapClass extends PersistentClass {
	public void init(Class clazz) {
		super.init(clazz);
		setNULL(create(PersistentObject.class));
	}

	protected boolean banned(String property) {
		return super.banned(property) || property.equals("empty");
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentClass.Accessor {
		public Accessor() throws RemoteException {}

		public PersistentObject NULL() {
			return getNULL();
		}
	}

	PersistentObject NULL() {
		return (PersistentObject)executeAtomic(
			new MethodCall("NULL",new Class[] {},new Object[] {}));
	}

	public PersistentObject getNULL() {
		return (PersistentObject)get("NULL");
	}

	public void setNULL(PersistentObject obj) {
		set("NULL",obj);
	}
}
