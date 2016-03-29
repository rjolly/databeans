import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import persistence.Store;

public class Sample {
	public static void main(String args[]) throws Exception {
		final Store store=new Store("heapspace");

		// Populate

		Collection employees=new java.util.ArrayList();

		Department accounting=(Department)store.create("Department");
		accounting.setName("Accounting");

		Department research=(Department)store.create("Department");
		research.setName("Research");

		Department sales=(Department)store.create("Department");
		sales.setName("Sales");

		Employee clark=(Employee)store.create("Employee");
		clark.setName("Clark");
		clark.setDepartment(accounting);
		clark.setLocation("New York");
		clark.setSalary(29400.);
		clark.setJob("Manager");
		employees.add(clark);

		Employee king=(Employee)store.create("Employee");
		king.setName("King");
		king.setDepartment(accounting);
		king.setLocation("New York");
		king.setSalary(60000.);
		king.setJob("President");
		employees.add(king);

		Employee miller=(Employee)store.create("Employee");
		miller.setName("Miller");
		miller.setDepartment(accounting);
		miller.setLocation("New York");
		miller.setSalary(15600.);
		miller.setJob("Clerk");
		employees.add(miller);

		Employee smith=(Employee)store.create("Employee");
		smith.setName("Smith");
		smith.setDepartment(research);
		smith.setLocation("New York");
		smith.setSalary(11400.);
		smith.setJob("Clerk");
		employees.add(smith);

		Employee adams=(Employee)store.create("Employee");
		adams.setName("Adams");
		adams.setDepartment(research);
		adams.setLocation("New York");
		adams.setSalary(11400.);
		adams.setJob("Clerk");
		employees.add(adams);

		Employee ford=(Employee)store.create("Employee");
		ford.setName("Ford");
		ford.setDepartment(research);
		ford.setLocation("New York");
		ford.setSalary(36000.);
		ford.setJob("Analyst");
		employees.add(ford);

		Employee scott=(Employee)store.create("Employee");
		scott.setName("Scott");
		scott.setDepartment(research);
		scott.setLocation("New York");
		scott.setSalary(36000.);
		scott.setJob("Analyst");
		employees.add(scott);

		Employee jones=(Employee)store.create("Employee");
		jones.setName("Jones");
		jones.setDepartment(research);
		jones.setLocation("New York");
		jones.setSalary(35700.);
		jones.setJob("Manager");
		employees.add(jones);

		Employee allen=(Employee)store.create("Employee");
		allen.setName("Allen");
		allen.setDepartment(sales);
		allen.setLocation("New York");
		allen.setSalary(16800.);
		allen.setJob("Salesman");
		employees.add(allen);

		Employee blake=(Employee)store.create("Employee");
		blake.setName("Blake");
		blake.setDepartment(sales);
		blake.setLocation("New York");
		blake.setSalary(34200.);
		blake.setJob("Manager");
		employees.add(blake);

		Employee martin=(Employee)store.create("Employee");
		martin.setName("Martin");
		martin.setDepartment(sales);
		martin.setLocation("New York");
		martin.setSalary(16800.);
		martin.setJob("Salesman");
		employees.add(martin);

		Employee james=(Employee)store.create("Employee");
		james.setName("James");
		james.setDepartment(sales);
		james.setLocation("New York");
		james.setSalary(11400.);
		james.setJob("Clerk");
		employees.add(james);

		Employee turner=(Employee)store.create("Employee");
		turner.setName("Turner");
		turner.setDepartment(sales);
		turner.setLocation("New York");
		turner.setSalary(16800.);
		turner.setJob("Salesman");
		employees.add(turner);

		Employee ward=(Employee)store.create("Employee");
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

		// Query

		NumberFormat format=NumberFormat.getCurrencyInstance();

		Employee average=(Employee)store.create("Employee");
		average.setName("");
		average.setSalary(24878.);

		for(Iterator it=employees.iterator();it.hasNext();) {
			Employee e=(Employee)it.next();
			if(e.getSalary()<average.getSalary())
				System.out.println(e+" "+format.format(e.getSalary()));
		}

		SortedSet bySalary=new TreeSet(SalaryComparator.comparator);
		bySalary.addAll(employees);

		System.out.println();

		for(Iterator it=bySalary.tailSet(average).iterator();it.hasNext();) {
			Employee e=(Employee)it.next();
			System.out.println(e+" "+format.format(e.getSalary()));
		}

		store.close();
	}
}
