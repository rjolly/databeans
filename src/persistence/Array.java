package persistence;

public interface Array {
	int length();
	char typeCode();
	boolean getBoolean(int index);
	void setBoolean(int index, boolean value);
	short getShort(int index);
	void setShort(int index, short value);
	char getChar(int index);
	void setChar(int index, char value);
	int getInt(int index);
	void setInt(int index, int value);
	long getLong(int index);
	void setLong(int index, long value);
	float getFloat(int index);
	void setFloat(int index, float value);
	double getDouble(int index);
	void setDouble(int index, double value);
	Object get(int index);
	void set(int index, Object value);
}
