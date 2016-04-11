import java.io.Serializable;
import java.util.Comparator;

public class SalaryComparator implements Comparator<Employee>, Serializable {
	public static final Comparator<Employee> comparator=new SalaryComparator();

	public int compare(Employee e1, Employee e2) {
		if(e1.getSalary()<e2.getSalary()) return -1;
		else if(e1.getSalary()>e2.getSalary()) return 1;
		else return e1.compareTo(e2);
	}
}
