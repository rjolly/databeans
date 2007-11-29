package persistence;

import java.rmi.RemoteException;

public class PersistentArrays {
	private PersistentArrays() {}

	public static RemoteArray remoteArray(Array a) {
		return ((LocalArray)a).a;
	}

	public static Array localArray(RemoteArray a) {
		return new LocalArray(a);
	}

	static class LocalArray implements Array {
		private RemoteArray a;

		LocalArray(RemoteArray a) {
			if (a==null)
				throw new NullPointerException();
			this.a = a;
		}

		public int length() {
			try {
				return a.length();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public char typeCode() {
			try {
				return a.typeCode();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean getBoolean(int index) {
			try {
				return a.getBoolean(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void setBoolean(int index, boolean value) {
			try {
				a.setBoolean(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public short getShort(int index) {
			try {
				return a.getShort(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void setShort(int index, short value) {
			try {
				a.setShort(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public char getChar(int index) {
			try {
				return a.getChar(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void setChar(int index, char value) {
			try {
				a.setChar(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public int getInt(int index) {
			try {
				return a.getInt(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void setInt(int index, int value) {
			try {
				a.setInt(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public long getLong(int index) {
			try {
				return a.getLong(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void setLong(int index, long value) {
			try {
				a.setLong(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public float getFloat(int index) {
			try {
				return a.getFloat(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void setFloat(int index, float value) {
			try {
				a.setFloat(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public double getDouble(int index) {
			try {
				return a.getDouble(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void setDouble(int index, double value) {
			try {
				a.setDouble(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public Object get(int index) {
			try {
				return a.get(index);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public void set(int index, Object value) {
			try {
				a.set(index,value);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		public String toString() {
			try {
				return a.remoteToString();
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void copy(Array src, int src_position, Array dst, int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst.set(dst_position+i,src.get(src_position+i));
		else for(int i=0;i<length;i++) dst.set(dst_position+i,src.get(src_position+i));
	}

	public static void copy(Object src[], int src_position, Array dst, int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst.set(dst_position+i,src[src_position+i]);
		else for(int i=0;i<length;i++) dst.set(dst_position+i,src[src_position+i]);
	}

	public static void copy(Array src, int src_position, Object dst[], int dst_position, int length) {
		if(src_position<dst_position) for(int i=length-1;i>=0;i--) dst[dst_position+i]=src.get(src_position+i);
		else for(int i=0;i<length;i++) dst[dst_position+i]=src.get(src_position+i);
	}
}
