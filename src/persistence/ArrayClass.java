package persistence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayClass extends PersistentClass {
	transient int header;
	char typeCode;
	int length;

	ArrayClass(Class componentType, int length) {
		super(PersistentArray.class);
		Field element=new Field("element",componentType);
		typeCode=element.typeCode;
		this.length=length;
		init(element,length);
	}

	private void init(Field element, int length) {
		header=size;
		size+=element.size*length;
	}

	void init(char typeCode, int length) {
		size=header;
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
		return Long.toHexString(base)+"["+length+" "+typeCode+"]";
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
	}

	private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
		s.defaultReadObject();
		init(new Field("element",typeCode),length);
	}
}
