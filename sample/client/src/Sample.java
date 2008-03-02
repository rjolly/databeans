import java.awt.Frame;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import persistence.Connection;
import persistence.Connections;
import persistence.util.TreeSet;

public class Sample {
	public static void main(String args[]) throws Exception {
		Frame frame=new Frame();
		Connection conn;
		try {
			conn=Connections.getConnection("//localhost/store");
			frame.dispose();
		} catch (Exception e) {
			frame.dispose();
			throw e;
		}

		// Populate

		conn.setRoot(conn.create("persistence.util.ArrayList"));
		Collection employees=(Collection)conn.root();

		Department accounting=(Department)conn.create("Department");
		accounting.setName("Accounting");

		Department research=(Department)conn.create("Department");
		research.setName("Research");

		Department sales=(Department)conn.create("Department");
		sales.setName("Sales");

		Employee clark=(Employee)conn.create("Employee");
		clark.setName("Clark");
		clark.setDepartment(accounting);
		clark.setLocation("New York");
		clark.setSalary(29400.);
		clark.setJob("Manager");
		employees.add(clark);

		Employee king=(Employee)conn.create("Employee");
		king.setName("King");
		king.setDepartment(accounting);
		king.setLocation("New York");
		king.setSalary(60000.);
		king.setJob("President");
		employees.add(king);

		Employee miller=(Employee)conn.create("Employee");
		miller.setName("Miller");
		miller.setDepartment(accounting);
		miller.setLocation("New York");
		miller.setSalary(15600.);
		miller.setJob("Clerk");
		employees.add(miller);

		Employee smith=(Employee)conn.create("Employee");
		smith.setName("Smith");
		smith.setDepartment(research);
		smith.setLocation("New York");
		smith.setSalary(11400.);
		smith.setJob("Clerk");
		employees.add(smith);

		Employee adams=(Employee)conn.create("Employee");
		adams.setName("Adams");
		adams.setDepartment(research);
		adams.setLocation("New York");
		adams.setSalary(11400.);
		adams.setJob("Clerk");
		employees.add(adams);

		Employee ford=(Employee)conn.create("Employee");
		ford.setName("Ford");
		ford.setDepartment(research);
		ford.setLocation("New York");
		ford.setSalary(36000.);
		ford.setJob("Analyst");
		employees.add(ford);

		Employee scott=(Employee)conn.create("Employee");
		scott.setName("Scott");
		scott.setDepartment(research);
		scott.setLocation("New York");
		scott.setSalary(36000.);
		scott.setJob("Analyst");
		employees.add(scott);

		Employee jones=(Employee)conn.create("Employee");
		jones.setName("Jones");
		jones.setDepartment(research);
		jones.setLocation("New York");
		jones.setSalary(35700.);
		jones.setJob("Manager");
		employees.add(jones);

		Employee allen=(Employee)conn.create("Employee");
		allen.setName("Allen");
		allen.setDepartment(sales);
		allen.setLocation("New York");
		allen.setSalary(16800.);
		allen.setJob("Salesman");
		employees.add(allen);

		Employee blake=(Employee)conn.create("Employee");
		blake.setName("Blake");
		blake.setDepartment(sales);
		blake.setLocation("New York");
		blake.setSalary(34200.);
		blake.setJob("Manager");
		employees.add(blake);

		Employee martin=(Employee)conn.create("Employee");
		martin.setName("Martin");
		martin.setDepartment(sales);
		martin.setLocation("New York");
		martin.setSalary(16800.);
		martin.setJob("Salesman");
		employees.add(martin);

		Employee james=(Employee)conn.create("Employee");
		james.setName("James");
		james.setDepartment(sales);
		james.setLocation("New York");
		james.setSalary(11400.);
		james.setJob("Clerk");
		employees.add(james);

		Employee turner=(Employee)conn.create("Employee");
		turner.setName("Turner");
		turner.setDepartment(sales);
		turner.setLocation("New York");
		turner.setSalary(16800.);
		turner.setJob("Salesman");
		employees.add(turner);

		Employee ward=(Employee)conn.create("Employee");
		ward.setName("Ward");
		ward.setDepartment(sales);
		ward.setLocation("New York");
		ward.setSalary(16800.);
		ward.setJob("Salesman");
		employees.add(ward);

		king.setManager(king);
		jones.setManager(king);
		scott.setManager(jones);
		adams.setManager(scott);
		ford.setManager(jones);
		smith.setManager(ford);
		blake.setManager(king);
		allen.setManager(blake);
		ward.setManager(blake);
		martin.setManager(blake);
		turner.setManager(blake);
		james.setManager(turner);
		clark.setManager(king);
		miller.setManager(clark);

		conn.commit();

		// Query

		NumberFormat format=NumberFormat.getCurrencyInstance();

		Employee average=(Employee)conn.create("Employee");
		average.setName("");
		average.setSalary(24878.);

		for(Iterator it=employees.iterator();it.hasNext();) {
			Employee e=(Employee)it.next();
			if(e.getSalary()<average.getSalary())
				System.out.println(e+" "+format.format(e.getSalary()));
		}

		SortedSet bySalary=(SortedSet)conn.create(TreeSet.class, new Class[] {Comparator.class}, new Object[] {SalaryComparator.comparator});
		bySalary.addAll(employees);

		System.out.println();

		for(Iterator it=bySalary.tailSet(average).iterator();it.hasNext();) {
			Employee e=(Employee)it.next();
			System.out.println(e+" "+format.format(e.getSalary()));
		}

		conn.close();
	}
}
