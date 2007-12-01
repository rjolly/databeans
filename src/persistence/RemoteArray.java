package persistence;

import java.rmi.RemoteException;

public interface RemoteArray extends Persistent {
	int length() throws RemoteException;
	char typeCode() throws RemoteException;
	boolean getBoolean(int index) throws RemoteException;
	void setBoolean(int index, boolean value) throws RemoteException;
	short getShort(int index) throws RemoteException;
	void setShort(int index, short value) throws RemoteException;
	char getChar(int index) throws RemoteException;
	void setChar(int index, char value) throws RemoteException;
	int getInt(int index) throws RemoteException;
	void setInt(int index, int value) throws RemoteException;
	long getLong(int index) throws RemoteException;
	void setLong(int index, long value) throws RemoteException;
	float getFloat(int index) throws RemoteException;
	void setFloat(int index, float value) throws RemoteException;
	double getDouble(int index) throws RemoteException;
	void setDouble(int index, double value) throws RemoteException;
	Object get(int index) throws RemoteException;
	void set(int index, Object value) throws RemoteException;
}
