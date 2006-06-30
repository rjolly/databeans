package persistence.storage;

import persistence.*;

public class StorageException extends PersistentException {
	public StorageException() {}

	public StorageException(String s) {
		super(s);
	}
}
