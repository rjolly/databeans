package persistence;

import java.util.*;

public class ArrayClass extends PersistentClass {
	transient char typeCode;
	transient int length;
	transient int header;

	ArrayClass(Class componentType, int length) {
		super(PersistentArray.class);
		init(new Field("element",componentType),length);
	}

	void init(Field element, int length) {
		typeCode=element.typeCode;
		this.length=length;
		header=size;
		size+=element.size*length;
	}

	void init(char typeCode, int length) {
		init(new Field("element",typeCode),length);
	}

	Field getField(int index) {
		if(index<length) {
			Field f=new Field("element["+index+"]",typeCode);
			f.setOffset(header+f.size*index);
			return f;
		} else throw new PersistentException("array index : "+index+" out of bounds : "+length);
	}

	Iterator fieldIterator() {
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
		return Long.toHexString(base)+(typeCode==0?"":"["+length+" "+typeCode+"]");
	}
}
