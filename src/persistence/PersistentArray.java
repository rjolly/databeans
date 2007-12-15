package persistence;

import java.rmi.RemoteException;
import persistence.PersistentObject.MethodCall;

public final class PersistentArray extends PersistentObject implements Array {
	public void init(Object component[]) {
		Arrays.copy(component,0,this,0,component.length);
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		public int length() {
			return getLength();
		}

		public char typeCode() {
			return getTypeCode();
		}

		public Object get(int index) {
			return get(((ArrayClass)clazz).getField(index));
		}

		public Object set(int index, Object value) {
			return set(((ArrayClass)clazz).getField(index),value);
		}

		public String remoteToString() {
			return dump();
		}
	}

	public int length() {
		return ((Integer)execute(
			new MethodCall("length",new Class[] {},new Object[] {}))).intValue();
	}

	public char typeCode() {
		return ((Character)execute(
			new MethodCall("typeCode",new Class[] {},new Object[] {}))).charValue();
	}

	public int getLength() {
		return ((Integer)get("length")).intValue();
	}

//	public void setLength(int n) {
//		set("length",new Integer(n));
//	}

	public char getTypeCode() {
		return ((Character)get("typeCode")).charValue();
	}

//	public void setTypeCode(char c) {
//		set("typeCode",new Character(c));
//	}

	public boolean getBoolean(int index) {
		return ((Boolean)get(index)).booleanValue();
	}

	public void setBoolean(int index, boolean value) {
		set(index,new Boolean(value));
	}

	public short getShort(int index) {
		return ((Short)get(index)).shortValue();
	}

	public void setShort(int index, short value) {
		set(index,new Short(value));
	}

	public char getChar(int index) {
		return ((Character)get(index)).charValue();
	}

	public void setChar(int index, char value) {
		set(index,new Character(value));
	}

	public int getInt(int index) {
		return ((Integer)get(index)).intValue();
	}

	public void setInt(int index, int value) {
		set(index,new Integer(value));
	}

	public long getLong(int index) {
		return ((Long)get(index)).longValue();
	}

	public void setLong(int index, long value) {
		set(index,new Long(value));
	}

	public float getFloat(int index) {
		return ((Float)get(index)).floatValue();
	}

	public void setFloat(int index, float value) {
		set(index,new Float(value));
	}

	public double getDouble(int index) {
		return ((Double)get(index)).doubleValue();
	}

	public void setDouble(int index, double value) {
		set(index,new Double(value));
	}

	public Object get(int index) {
		return execute(
			new MethodCall("get",new Class[] {int.class},new Object[] {new Integer(index)}));
	}

	public void set(int index, Object value) {
		execute(
			new MethodCall("set",new Class[] {int.class,Object.class},new Object[] {new Integer(index),value}),
			new MethodCall("set",new Class[] {int.class,Object.class},new Object[] {new Integer(index),null}),1);
	}

	public String dump() {
		StringBuffer s=new StringBuffer();
		s.append("{");
		int n=length();
		for(int i=0;i<n;i++) s.append((i==0?"":", ")+get(i));
		s.append("}");
		return s.toString();
	}
}
