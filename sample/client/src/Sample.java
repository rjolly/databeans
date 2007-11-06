import java.util.*;
import persistence.*;
import persistence.util.*;

public class Sample {
	public static void main(String args[]) throws Exception {
		if (System.getSecurityManager() == null) System.setSecurityManager(new SecurityManager());
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
		RemoteCollection employees=(RemoteCollection)conn.create("persistence.util.PersistentArrayList");
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
		System.out.println(PersistentCollections.localCollection((RemoteCollection)conn.getRoot()));
		conn.commit();
	}
}
