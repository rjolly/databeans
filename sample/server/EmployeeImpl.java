import java.rmi.*;
import persistence.*;

import java.text.NumberFormat;

public class EmployeeImpl extends PersistentObject implements Employee {
	public EmployeeImpl() throws RemoteException {}

	public String getName() {
		return (String)get("name");
	}

	public void setName(String s) {
		set("name",s);
	}

	public Department getDepartment() {
		return (Department)get("department");
	}

	public void setDepartment(Department d) {
		set("department",d);
	}

	public String getLocation() {
		return (String)get("location");
	}

	public void setLocation(String s) {
		set("location",s);
	}

	public double getSalary() {
		return ((Double)get("salary")).doubleValue();
	}

	public void setSalary(double d) {
		set("salary",new Double(d));
	}

	public Employee getManager() {
		return (Employee)get("manager");
	}

	public void setManager(Employee e) {
		set("manager",e);
	}

	public String getJob() {
		return (String)get("job");
	}

	public void setJob(String s) {
		set("job",s);
	}

	public String remoteToString() throws RemoteException {
		return "["+getName()+" "+getDepartment()+" "+getLocation()+" "+getJob()+" "+NumberFormat.getCurrencyInstance().format(getSalary())+" "+getManager().getName()+"]";
	}
}
