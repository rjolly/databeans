import java.beans.ConstructorProperties;
import persistence.PersistentObject;
import persistence.Store;

public class Employee extends PersistentObject implements Comparable<Employee> {
	public Employee() {
	}

	@ConstructorProperties({"name"})
	public Employee(final Store store, final String name) {
		super(store);
		set("name", name);
	}

	public String getName() {
		return get("name");
	}

	public Department getDepartment() {
		return get("department");
	}

	public void setDepartment(Department d) {
		set("department",d);
	}

	public String getLocation() {
		return get("location");
	}

	public void setLocation(String s) {
		set("location",s);
	}

	public double getSalary() {
		return get("salary");
	}

	public void setSalary(double d) {
		set("salary",d);
	}

	public Employee getManager() {
		return get("manager");
	}

	public void setManager(Employee e) {
		set("manager",e);
	}

	public String getJob() {
		return get("job");
	}

	public void setJob(String s) {
		set("job",s);
	}

	public String toString() {
		return getName()+" ("+getDepartment()+")";
	}

	public int compareTo(Employee e) {
		return getName().compareTo(e.getName());
	}
}
