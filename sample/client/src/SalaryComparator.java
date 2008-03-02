import java.io.Serializable;
import java.util.Comparator;

public class SalaryComparator implements Comparator, Serializable {
	public static final Comparator comparator=new SalaryComparator();

	public int compare(Object o1, Object o2) {
		Employee e1=(Employee)o1;
		Employee e2=(Employee)o2;
		if(e1.getSalary()<e2.getSalary()) return -1;
		else if(e1.getSalary()>e2.getSalary()) return 1;
		else return e1.compareTo(e2);
	}
}
