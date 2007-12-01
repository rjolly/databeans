import java.rmi.RemoteException;
import persistence.Persistent;

public interface Department extends Persistent {
	String getName() throws RemoteException;
	void setName(String s) throws RemoteException;
}
