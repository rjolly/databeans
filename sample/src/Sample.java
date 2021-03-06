import java.text.NumberFormat;
import java.util.Collection;
import java.util.SortedSet;
import persistence.Store;
import persistence.util.ArrayList;
import persistence.util.TreeSet;

public class Sample {
	public static void main(String args[]) {
		try (final Store store=new Store("heapspace")) {

			// Populate

			store.setRoot(new ArrayList<Employee>(store));
			Collection<Employee> employees=store.root();

			Department accounting=new Department(store, "Accounting");
			Department research=new Department(store, "Research");
			Department sales=new Department(store, "Sales");

			Employee clark=new Employee(store, "Clark");
			clark.setDepartment(accounting);
			clark.setLocation("New York");
			clark.setSalary(29400.);
			clark.setJob("Manager");
			employees.add(clark);

			Employee king=new Employee(store, "King");
			king.setDepartment(accounting);
			king.setLocation("New York");
			king.setSalary(60000.);
			king.setJob("President");
			employees.add(king);

			Employee miller=new Employee(store, "Miller");
			miller.setDepartment(accounting);
			miller.setLocation("New York");
			miller.setSalary(15600.);
			miller.setJob("Clerk");
			employees.add(miller);

			Employee smith=new Employee(store, "Smith");
			smith.setDepartment(research);
			smith.setLocation("New York");
			smith.setSalary(11400.);
			smith.setJob("Clerk");
			employees.add(smith);

			Employee adams=new Employee(store, "Adams");
			adams.setDepartment(research);
			adams.setLocation("New York");
			adams.setSalary(11400.);
			adams.setJob("Clerk");
			employees.add(adams);

			Employee ford=new Employee(store, "Ford");
			ford.setDepartment(research);
			ford.setLocation("New York");
			ford.setSalary(36000.);
			ford.setJob("Analyst");
			employees.add(ford);

			Employee scott=new Employee(store, "Scott");
			scott.setDepartment(research);
			scott.setLocation("New York");
			scott.setSalary(36000.);
			scott.setJob("Analyst");
			employees.add(scott);

			Employee jones=new Employee(store, "Jones");
			jones.setDepartment(research);
			jones.setLocation("New York");
			jones.setSalary(35700.);
			jones.setJob("Manager");
			employees.add(jones);

			Employee allen=new Employee(store, "Allen");
			allen.setDepartment(sales);
			allen.setLocation("New York");
			allen.setSalary(16800.);
			allen.setJob("Salesman");
			employees.add(allen);

			Employee blake=new Employee(store, "Blake");
			blake.setDepartment(sales);
			blake.setLocation("New York");
			blake.setSalary(34200.);
			blake.setJob("Manager");
			employees.add(blake);

			Employee martin=new Employee(store, "Martin");
			martin.setDepartment(sales);
			martin.setLocation("New York");
			martin.setSalary(16800.);
			martin.setJob("Salesman");
			employees.add(martin);

			Employee james=new Employee(store, "James");
			james.setDepartment(sales);
			james.setLocation("New York");
			james.setSalary(11400.);
			james.setJob("Clerk");
			employees.add(james);

			Employee turner=new Employee(store, "Turner");
			turner.setDepartment(sales);
			turner.setLocation("New York");
			turner.setSalary(16800.);
			turner.setJob("Salesman");
			employees.add(turner);

			Employee ward=new Employee(store, "Ward");
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

			Employee average=new Employee(store, "");
			average.setSalary(24878.);

			for(final Employee e : employees) {
				if(e.getSalary()<average.getSalary())
					System.out.println(e+" "+format.format(e.getSalary()));
			}

			SortedSet<Employee> bySalary=new TreeSet<>(store, SalaryComparator.comparator);
			bySalary.addAll(employees);

			System.out.println();

			for(final Employee e : bySalary.tailSet(average)) {
				System.out.println(e+" "+format.format(e.getSalary()));
			}
		}
	}
}
