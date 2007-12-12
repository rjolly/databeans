import java.awt.Frame;
import java.util.Collection;
import persistence.Connection;
import persistence.Connections;

public class Sample {
	public static void main(String args[]) throws Exception {
		Frame frame=new Frame();
		Connection conn;
		try {
			conn=Connections.getConnection(frame,"//localhost/store");
			frame.dispose();
		} catch (Exception e) {
			frame.dispose();
			throw e;
		}
		Department d=(Department)conn.create("Department");
		d.setName("Research");
		Employee e=(Employee)conn.create("Employee");
		e.setName("Clark");
		e.setDepartment(d);
		e.setLocation("New York");
		e.setSalary(2000.);
		e.setManager(e);
		e.setJob("Manager");
		Collection employees=(Collection)conn.create("persistence.util.PersistentArrayList");
		employees.add(e);
		conn.setRoot(employees);
		Employee m=e;
		e=(Employee)conn.create("Employee");
		e.setName("Miller");
		e.setDepartment(m.getDepartment());
		e.setLocation("New York");
		e.setSalary(1000.);
		e.setManager(m);
		e.setJob("Clerk");
		employees.add(e);
		System.out.println(conn.getRoot());
		conn.commit();
		conn.close();
	}
}
