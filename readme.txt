
How does it work ?

Take a class that follows the Bean pattern, with its private instance variables and public accessor methods.

public class Employee {
	private String name;
	private String job;

	public Employee(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public String toString() {
		return name;
	}
}

Extend the PerstistentObject class:

public class Employee extends PersistentObject {
}

Replace the field accesses by calls to PersistentObject's get and set methods:

	public String getJob() {
		return get("job");
	}

	public void setJob(String job) {
		set("job", job);
	}

Add a "store" argument to the beginning of the constructor, and replace the field access here, in the read-only accessor, and in the toString method:

	public Employee(Store store, String name) {
		super(store);
		set("name", name);
	}

	public String getName() {
		return get("name");
	}

	public String toString() {
		return get("name");
	}

Add a public, no-arg constructor for re-instantiation from the persistent storage:

	public Employee() {
	}

That's it, we have coded our first databean.


How to use ?

Open the store:

		Store store = new Store("heapspace");

Create a new employee:

		Employee clark = new Employee(store, "Clark");

Attach it to the root of the store:

		store.setRoot(clark);

Exit the application:

		store.close();

Re-run the application with:

		Store store = new Store("heapspace");
		System.out.println(store.root()); // prints "Clark" : the data was persisted across application runs.


Required software

- jdk 1.7 ( http://www.oracle.com/technetwork/java/index.html )

Optional software

- Eclipse 4.4 ( http://www.eclipse.org/ )


To run the sample, first create the heapspace:
  run as java application : persistence.server.MakeHeapSpace


Then just:
  run as java application : Sample


To use in your project:
  add net.sourceforge.databeans#databeans;3.0 to your dependencies

