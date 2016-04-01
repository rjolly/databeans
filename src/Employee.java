import persistence.PersistentObject;
import persistence.Store;

public class Employee extends PersistentObject implements Comparable {
	public Employee() {
	}

	public Employee(final Store store) {
		super(store);
	}

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

	public String toString() {
		return getName()+" ("+getDepartment()+")";
	}

	public int compareTo(Object o) {
		return getName().compareTo(((Employee)o).getName());
	}
}
