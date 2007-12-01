import java.rmi.RemoteException;
import persistence.Persistent;

public interface Employee extends Persistent {
	public String getName() throws RemoteException;
	public void setName(String s) throws RemoteException;
	public Department getDepartment() throws RemoteException;
	public void setDepartment(Department d) throws RemoteException;
	public String getLocation() throws RemoteException;
	public void setLocation(String s) throws RemoteException;
	public double getSalary() throws RemoteException;
	public void setSalary(double d) throws RemoteException;
	public Employee getManager() throws RemoteException;
	public void setManager(Employee e) throws RemoteException;
	public String getJob() throws RemoteException;
	public void setJob(String s) throws RemoteException;
}
