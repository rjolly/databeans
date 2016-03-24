package persistence;

public final class PersistentArray extends PersistentObject implements Array {
	public void init(Object component[]) {
		copy(component,0,this,0,component.length);
	}

	protected PersistentClass createClass() {
		return createClass(int.class,0);
	}

	PersistentClass createClass(Class componentType, int length) {
		return (PersistentClass)create(ArrayClass.class,new Class[] {Class.class,Class.class,int.class},new Object[] {getClass(),componentType,new Integer(length)});
	}

	public int length() {
		return ((ArrayClass)clazz).getLength();
	}

	public char typeCode() {
		return ((ArrayClass)clazz).getTypeCode();
	}

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
		return get(((ArrayClass)clazz).getField(index));
	}

	public void set(int index, Object value) {
		set(((ArrayClass)clazz).getField(index),value);
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

	public String toString() {
		StringBuffer s=new StringBuffer();
		s.append("{");
		int n=length();
		for(int i=0;i<n;i++) s.append((i==0?"":", ")+get(i));
		s.append("}");
		return s.toString();
	}
}
