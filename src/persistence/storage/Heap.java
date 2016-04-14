package persistence.storage;

import java.util.Iterator;

public interface Heap {
	long boot();
	void setBoot(long ptr);
	boolean mounted();
	void mount(boolean n);
	void close();
	long alloc(int size);
	long realloc(long ptr, int size);
	void free(long ptr);
	boolean mark(long ptr, boolean n);
	boolean status(long ptr);
	long allocatedSpace();
	long maxSpace();
	Iterator<Long> iterator();

	boolean readBoolean(long ptr);
	byte readByte(long ptr);
	short readShort(long ptr);
	char readChar(long ptr);
	int readInt(long ptr);
	long readLong(long ptr);
	float readFloat(long ptr);
	double readDouble(long ptr);
	byte[] readBytes(long ptr);

	void writeBoolean(long ptr, boolean v);
	void writeByte(long ptr, int v);
	void writeShort(long ptr, int v);
	void writeChar(long ptr, int v);
	void writeInt(long ptr, int v);
	void writeLong(long ptr, long v);
	void writeFloat(long ptr, float v);
	void writeDouble(long ptr, double v);
	void writeBytes(long ptr, byte b[]);
}
