package persistence;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import persistence.PersistentObject.MethodCall;

public class PersistentClass extends PersistentObject {
	transient Map map;
	transient int size;

	public void init(Class clazz) {
		if(!PersistentObject.class.isAssignableFrom(clazz)) throw new PersistentException("type not persistent");
		BeanInfo info;
		try {
			info=Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		PropertyDescriptor desc[]=info.getPropertyDescriptors();
		Collection c=new ArrayList();
		for(int i=0;i<desc.length;i++) {
			PropertyDescriptor d=desc[i];
			if(d instanceof IndexedPropertyDescriptor) continue;
			if(d.getName().equals("class")) continue;
			c.add(new Field(d));
		}
		Field fields[];
		c.toArray(fields=new Field[c.size()]);
		setFields(create(fields));
		setName(clazz.getName());
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		public String name() {
			return getName();
		}

		public String remoteToString() {
			StringBuffer s=new StringBuffer();
			Array fields=getFields();
			s.append(Long.toHexString(base.longValue()));
			s.append("[");
			for(int i=0;i<fields.length();i++) s.append((i==0?"":", ")+fields.get(i));
			s.append("]");
			return s.toString();
		}
	}

	protected PersistentClass createClass() {
		return (PersistentClass)create(ClassClass.class,new Class[] {Class.class},new Object[] {getClass()});
	}

	protected static class ClassClass extends PersistentClass {
		PersistentObject newInstance() {
			return this;
		}
	}

	protected PersistentClass() {}

	void setup() {
		size=Field.HEADER_SIZE;
		map=new HashMap();
		Array fields=getFields();
		for(int i=0;i<fields.length();i++) {
			Field f=(Field)fields.get(i);
			f.setOffset(size);
			map.put(f.name, f);
			size+=f.size;
		}
	}

	int size() {
		if(map==null) setup();
		return size;
	}

	Field getField(String name) {
		if(map==null) setup();
		Field f=(Field)map.get(name);
		if(f==null) throw new PersistentException("no such property : "+name+" in class : "+getName());
		return f;
	}

	Iterator fieldIterator() {
		if(map==null) setup();
		return map.values().iterator();
	}

	static PersistentClass create(Class clazz, StoreImpl store) {
		PersistentObject obj=newInstance(clazz);
		obj.connection=store.systemConnection;
		return obj.createClass();
	}

	static PersistentClass create(Class clazz, int length, StoreImpl store) {
		PersistentArray obj=new PersistentArray();
		obj.connection=store.systemConnection;
		return obj.createClass(clazz,length);
	}

	static PersistentObject newInstance(Class clazz) {
		try {
			return (PersistentObject)clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	PersistentObject newInstance() {
		try {
			return newInstance(Class.forName(getName()));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public String name() {
		return (String)execute(
			new MethodCall("name",new Class[] {},new Object[] {}));
	}

	public String getName() {
		return (String)get("name");
	}

	public void setName(String str) {
		set("name",str);
	}

	public Array getFields() {
		return (Array)get("fields");
	}

	public void setFields(Array array) {
		set("fields",array);
	}
}
