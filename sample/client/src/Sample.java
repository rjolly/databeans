import java.util.Collection;
import persistence.Connection;
import persistence.Connections;

public class Sample {
	public static void main(String args[]) throws Exception {
		Connection conn=Connections.getConnection("//localhost/store");
		Department d=(Department)conn.create("DepartmentImpl");
		d.setName("Research");
		Employee e=(Employee)conn.create("EmployeeImpl");
		e.setName("Clark");
		e.setDepartment(d);
		e.setLocation("New York");
		e.setSalary(2000.);
		e.setManager(e);
		e.setJob("Manager");
		Collection employees=(Collection)conn.create("persistence.util.PersistentArrayList");
		employees.add(e);
		conn.setRoot(employees);
		Employee m=(Employee)employees.iterator().next();
		e=(Employee)conn.create("EmployeeImpl");
		e.setName("Miller");
		e.setDepartment(m.getDepartment());
		e.setLocation("New York");
		e.setSalary(1000.);
		e.setManager(m);
		e.setJob("Clerk");
		employees.add(e);
		System.out.println(conn.getRoot());
		conn.commit();
	}
}
