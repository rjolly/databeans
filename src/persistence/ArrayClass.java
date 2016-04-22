package persistence;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ArrayClass<C> extends PersistentClass {
	transient int header;

	public ArrayClass() {
	}

	ArrayClass(final Store store, Class<C> componentType, int length) {
		this(store, new Field("element",componentType).typeCode, length);
	}

	ArrayClass(final Store store, char typeCode, int length) {
		super(store, PersistentArray.class);
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

	Iterator<Field> fieldIterator() {
		if(map==null) setup();
		return new Iterator<Field>() {
			int length=getLength();
			int index=0;

			public boolean hasNext() {
				return index<length;
			}

			public Field next() {
				if (index>=length) throw new NoSuchElementException();
				return getField(index++);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public int getLength() {
		return get("length");
	}

	public void setLength(int n) {
		set("length",n);
	}

	public char getTypeCode() {
		return get("typeCode");
	}

	public void setTypeCode(char c) {
		set("typeCode",c);
	}
}
