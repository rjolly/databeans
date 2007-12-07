package persistence.server;

import java.io.Serializable;
import java.security.Principal;

public class DatabeansPrincipal implements Principal, Serializable {

	private String name;

	public DatabeansPrincipal(String name) {
		if (name == null)
			throw new NullPointerException("illegal null input");

		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return("DatabeansPrincipal:  " + name);
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;

		if (this == o)
			return true;

		if (!(o instanceof DatabeansPrincipal))
			return false;
		DatabeansPrincipal that = (DatabeansPrincipal)o;

		if (this.getName().equals(that.getName()))
			return true;
		return false;
	}

	public int hashCode() {
		return name.hashCode();
	}
}
