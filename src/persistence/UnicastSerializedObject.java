package persistence;

import java.io.Serializable;

class UnicastSerializedObject implements Serializable {
	transient long base;

	public final int hashCode() {
		return new Long(base).hashCode();
	}

	public final boolean equals(Object obj) {
		return this == obj || (obj instanceof UnicastSerializedObject && base==((UnicastSerializedObject)obj).base);
	}

	public String toString() {
		return getClass().getName() + "@" + Long.toHexString(base);
	}
}
