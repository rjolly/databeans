package persistence.util;

import java.rmi.RemoteException;
import persistence.PersistentObject;

public final class HashSetClass extends AbstractCollectionClass {
	public void init(Class clazz) {
		super.init(clazz);
		setPRESENT(create(PersistentObject.class));
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends AbstractCollectionClass.Accessor {
		public Accessor() throws RemoteException {}

		public PersistentObject PRESENT() {
			return getPRESENT();
		}
	}

	// Dummy value to associate with an Object in the backing Map
	PersistentObject PRESENT() {
		return (PersistentObject)execute(
			new MethodCall("PRESENT",new Class[] {},new Object[] {}));
	}

	public PersistentObject getPRESENT() {
		return (PersistentObject)get("PRESENT");
	}

	public void setPRESENT(PersistentObject obj) {
		set("PRESENT",obj);
	}
}
