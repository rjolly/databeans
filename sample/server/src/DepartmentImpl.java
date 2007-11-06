import java.rmi.*;
import persistence.*;

public class DepartmentImpl extends PersistentObject implements Department {
	public DepartmentImpl() throws RemoteException {}

	public String getName() {
		return (String)get("name");
	}

	public void setName(String s) {
		set("name",s);
	}

	public String remoteToString() {
		return getName();
	}
}
