package persistence;

public class PersistentException extends RuntimeException {
	public PersistentException() {}

	public PersistentException(String s) {
		super(s);
	}
}
