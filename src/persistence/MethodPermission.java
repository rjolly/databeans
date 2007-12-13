package persistence;

import java.security.BasicPermission;

public class MethodPermission extends BasicPermission {
	public MethodPermission(String name) {
		super(name);
	}
}
