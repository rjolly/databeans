package persistence;

import java.security.BasicPermission;

public class PropertyPermission extends BasicPermission {
	public PropertyPermission(String name) {
		super(name);
	}
}
