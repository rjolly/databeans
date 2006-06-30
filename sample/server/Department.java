import java.rmi.*;

public interface Department extends Remote {
	String getName() throws RemoteException;
	void setName(String s) throws RemoteException;
	String remoteToString() throws RemoteException;
	String persistentClass() throws RemoteException;
}
