package persistence;

import java.security.BasicPermission;

public class AdminPermission extends BasicPermission {
	public AdminPermission(String name) {
		super(name);
	}
}
