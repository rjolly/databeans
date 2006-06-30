package persistence;

import java.io.*;
import java.beans.*;
import persistence.storage.*;

public class Field implements Serializable {
	static final Field REF_COUNT=new Field("refCount",'L');
	static final Field CLASS=new Field("class",'L');
	static final Field LOCK=new Field("lock",'L');
	static final int HEADER_SIZE;
	static {
		Field f[]=new Field[] {REF_COUNT,CLASS,LOCK};
		int size=0;
		for(int i=0;i<f.length;i++) {
			f[i].setOffset(size);
			size+=f[i].size;
		}
		HEADER_SIZE=size;
	}
	String name;
	char typeCode;
	transient int size;
	transient int offset;
	transient boolean reference;

	Field(PropertyDescriptor desc) {
		this(desc.getName(), desc.getPropertyType());
	}

	Field(String name, Class type) {
		this(name, new ObjectStreamField(name,type).getTypeCode());
	}

	Field(String name, char typeCode) {
		this.name=name;
		this.typeCode=typeCode;
		init();
	}

	private void init() {
		switch (typeCode) {
		case 'Z':
		case 'B':
			size=1;
			reference=false;
			break;
		case 'C':
		case 'S':
			size=2;
			reference=false;
			break;
		case 'I':
		case 'F':
			size=4;
			reference=false;
			break;
		case 'J':
		case 'D':
			size=8;
			reference=false;
			break;
		case '[':
		case 'L':
			size=MemoryModel.model.pointerSize;
			reference=true;
			break;
		default:
			throw new PersistentException("internal error");
		}
	}

	void setOffset(int offset) {
		this.offset=offset;
	}

	Object get(Heap heap, long base) {
		switch (typeCode) {
		case 'Z':
			return new Boolean(heap.readBoolean(base+offset));
		case 'B':
			return new Byte(heap.readByte(base+offset));
		case 'C':
			return new Character(heap.readChar(base+offset));
		case 'S':
			return new Short(heap.readShort(base+offset));
		case 'I':
			return new Integer(heap.readInt(base+offset));
		case 'F':
			return new Float(heap.readFloat(base+offset));
		case 'J':
			return new Long(heap.readLong(base+offset));
		case 'D':
			return new Double(heap.readDouble(base+offset));
		case '[':
		case 'L':
			return new Long(MemoryModel.model.readPointer(heap,base+offset));
		default:
			throw new PersistentException("internal error");
		}
	}

	void set(Heap heap, long base, Object value) {
		switch (typeCode) {
		case 'Z':
			heap.writeBoolean(base+offset,((Boolean)value).booleanValue());
			break;
		case 'B':
			heap.writeByte(base+offset,((Byte)value).byteValue());
			break;
		case 'C':
			heap.writeChar(base+offset,((Character)value).charValue());
			break;
		case 'S':
			heap.writeShort(base+offset,((Short)value).shortValue());
			break;
		case 'I':
			heap.writeInt(base+offset,((Integer)value).intValue());
			break;
		case 'F':
			heap.writeFloat(base+offset,((Float)value).floatValue());
			break;
		case 'J':
			heap.writeLong(base+offset,((Long)value).longValue());
			break;
		case 'D':
			heap.writeDouble(base+offset,((Double)value).doubleValue());
			break;
		case '[':
		case 'L':
			MemoryModel.model.writePointer(heap,base+offset,((Long)value).longValue());
			break;
		default:
			throw new PersistentException("internal error");
		}
	}

	public String toString() {
		return name+" "+typeCode;
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
	}

	private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
		s.defaultReadObject();
		init();
	}
}
