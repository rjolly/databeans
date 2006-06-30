package persistence;

import java.security.*;

public class User extends UnicastSerializedObject implements Principal {
	String name;
	byte[] password;

	User(String username, String password) {
		this(username,Connections.crypt(password));
	}

	User(String name, byte[] password) {
		this.name=name;
		this.password=password;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return Long.toHexString(base);
	}
}
