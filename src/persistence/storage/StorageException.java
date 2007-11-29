package persistence.storage;

import persistence.PersistentException;

public class StorageException extends PersistentException {
	public StorageException() {}

	public StorageException(String s) {
		super(s);
	}
}
