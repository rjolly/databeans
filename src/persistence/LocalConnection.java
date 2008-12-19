package persistence;

public class LocalConnection extends Connection {

	LocalConnection(RemoteConnection connection) {
		super(connection);
	}

	PersistentObject attach(PersistentObject obj) {
		return super.attach(obj.connection==null?obj:PersistentObject.newInstance(obj));
	}
}
