package persistence;

import java.rmi.RemoteException;

public final class PersistentArray extends PersistentObject implements RemoteArray {
	public PersistentArray() throws RemoteException {}

	public PersistentArray(Accessor accessor, Connection connection) throws RemoteException {
		super(accessor,connection);
	}

	public PersistentArray(Accessor accessor, Connection connection, Object component[]) throws RemoteException {
		super(accessor,connection);
		PersistentArrays.copy(component,0,PersistentArrays.localArray(this),0,component.length);
	}

	public int length() {
		return getLength();
	}

	public char typeCode() {
		return getTypeCode();
	}

	public char getTypeCode() {
		return ((Character)get("typeCode")).charValue();
	}

//	public void setTypeCode(char c) {
//		set("typeCode",new Character(c));
//	}

	public int getLength() {
		return ((Integer)get("length")).intValue();
	}

//	public void setLength(int n) {
//		set("length",new Integer(n));
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
		return connection.call(this,"get",new Class[] {Integer.class},new Object[] {new Integer(index)});
	}

	public void set(int index, Object value) {
		connection.call(this,
			"set",new Class[] {Integer.class,Object.class},new Object[] {new Integer(index),value},
			"set",new Class[] {Integer.class,Object.class},new Object[] {new Integer(index),null},1);
	}

	Object getImpl(Integer index) {
		return get(((ArrayClass)accessor.clazz).getField(index.intValue()));
	}

	Object setImpl(Integer index, Object value) {
		return set(((ArrayClass)accessor.clazz).getField(index.intValue()),value);
	}

	public String remoteToString() throws RemoteException {
		StringBuffer s=new StringBuffer();
		s.append("{");
		int n=length();
		for(int i=0;i<n;i++) s.append((i==0?"":", ")+get(i));
		s.append("}");
		return s.toString();
	}
}
