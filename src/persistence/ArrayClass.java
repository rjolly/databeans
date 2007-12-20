package persistence;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class ArrayClass extends PersistentClass {
	transient int header;

	public void init(Class clazz, Class componentType, int length) {
		init(clazz);
		setTypeCode(new Field("element",componentType).typeCode);
		setLength(length);
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentClass.Accessor {
		public Accessor() throws RemoteException {}
		
		public String remoteToString() {
			return Long.toHexString(base().longValue())+name(getLength(),getTypeCode());
		}
	}

	static String name(Class componentType, int length) {
		return name(length,new Field("element",componentType).typeCode);
	}

	static String name(int length, char typeCode) {
		return "["+length+" "+typeCode+"]";
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(PersistentClass.class,new Class[] {Class.class},new Object[] {getClass()});
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
		} else throw new PersistentException("array index : "+index+" out of bounds : "+length);
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

	static PersistentClass create(Class componentType, int length, StoreImpl store) {
		PersistentArray obj=new PersistentArray();
		obj.connection=store.systemConnection;
		return obj.createClass(componentType,length);
	}
}
