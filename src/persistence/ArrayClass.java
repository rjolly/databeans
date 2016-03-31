package persistence;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ArrayClass extends PersistentClass {
	transient int header;

	public ArrayClass() {
	}

	public ArrayClass(final Store store, Class clazz, Class componentType, int length) {
		this(store, clazz,new Field("element",componentType).typeCode,length);
	}

	ArrayClass(final Store store, Class clazz, char typeCode, int length) {
		super(store, clazz);
		setTypeCode(typeCode);
		setLength(length);
	}

	void setup() {
		super.setup();
		header=size;
		size+=new Field("element",getTypeCode()).size*getLength();
	}

	Field getField(int index) {
		if(map==null) setup();
		int length=getLength();
		if(index<length) {
			Field f=new Field("element["+index+"]",getTypeCode());
			f.setOffset(header+f.size*index);
			return f;
		} else throw new RuntimeException("array index : "+index+" out of bounds : "+length);
	}

	Iterator fieldIterator() {
		if(map==null) setup();
		return new Iterator() {
			int length=getLength();
			int index=0;

			public boolean hasNext() {
				return index<length;
			}

			public Object next() {
				if (index>=length) throw new NoSuchElementException();
				return getField(index++);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public int getLength() {
		return ((Integer)get("length")).intValue();
	}

	public void setLength(int n) {
		set("length",new Integer(n));
	}

	public char getTypeCode() {
		return ((Character)get("typeCode")).charValue();
	}

	public void setTypeCode(char c) {
		set("typeCode",new Character(c));
	}

	static PersistentClass create(Class componentType, int length, Store store) {
		PersistentArray obj=new PersistentArray();
		obj.store=store;
		return obj.createClass(componentType,length);
	}
}
