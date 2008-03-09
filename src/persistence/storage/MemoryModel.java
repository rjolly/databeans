package persistence.storage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MemoryModel {
	public static final MemoryModel model=new MemoryModel(64);
	public final int pointerSize;
	public final int lastByteShift;
	public final long lastByteMask;

	private MemoryModel(int n) {
		pointerSize=n>>3;
		lastByteShift=n-8;
		lastByteMask=0xffl<<lastByteShift;
	}

	public long readPointer(Heap heap, long ptr) {
		return pointerSize==4?heap.readInt(ptr):heap.readLong(ptr);
	}

	public void writePointer(Heap heap, long ptr, long v) {
		if(pointerSize==4) heap.writeInt(ptr,(int)v);
		else heap.writeLong(ptr,v);
	}

	public long readPointer(DataInput s) {
		try {
			return pointerSize==4?s.readInt():s.readLong();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writePointer(DataOutput s, long v) {
		try {
			if(pointerSize==4) s.writeInt((int)v);
			else s.writeLong(v);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public long pointerValue(Number n) {
		return n==null?0:pointerSize==4?((Integer)n).intValue():((Long)n).longValue();
	}

	public Number toNumber(long v) {
		return v==0?null:pointerSize==4?(Number)new Integer((int)v):new Long(v);
	}
}
