package persistence;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayClass extends PersistentClass {
	transient int header;
	char typeCode;
	int length;

	ArrayClass(Class componentType, int length) {
		super(PersistentArray.class);
		init(new Field("element",componentType).typeCode,length);
	}

	void init(char typeCode, int length) {
		this.typeCode=typeCode;
		this.length=length;
	}

	void init() {
		super.init();
		header=size;
		size+=new Field("element",typeCode).size*length;
	}

	Field getField(int index) {
		if(map==null) init();
		if(index<length) {
			Field f=new Field("element["+index+"]",typeCode);
			f.setOffset(header+f.size*index);
			return f;
		} else throw new PersistentException("array index : "+index+" out of bounds : "+length);
	}

	Iterator fieldIterator() {
		if(map==null) init();
		return new Iterator() {
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

	public String toString() {
		return Long.toHexString(base)+"["+length+" "+typeCode+"]";
	}
}
