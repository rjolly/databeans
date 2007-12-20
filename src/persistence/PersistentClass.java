package persistence;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PersistentClass extends PersistentObject {
	transient Map map;
	transient int size;
	transient String name;

	public void init(Class clazz) {
		if(!PersistentObject.class.isAssignableFrom(clazz)) throw new PersistentException("type not persistent");
		BeanInfo info;
		try {
			info=Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		PropertyDescriptor desc[]=info.getPropertyDescriptors();
		StringBuffer buffer=new StringBuffer();
		boolean first=true;
		for(int i=0;i<desc.length;i++) {
			PropertyDescriptor d=desc[i];
			if(d instanceof IndexedPropertyDescriptor) continue;
			if(d.getName().equals("class")) continue;
			buffer.append(first?"":";").append(new Field(d));
			first=false;
		}
		setFields(buffer.toString());
		setName(clazz.getName());
	}

	protected PersistentObject.Accessor createAccessor() throws RemoteException {
		return new Accessor();
	}

	protected class Accessor extends PersistentObject.Accessor {
		public Accessor() throws RemoteException {}

		public String remoteToString() {
			StringBuffer s=new StringBuffer();
			String fields[]=getFields().split(";");
			s.append(Long.toHexString(base.longValue()));
			s.append("[");
			for(int i=0;i<fields.length;i++) s.append((i==0?"":", ")+fields[i]);
			s.append("]");
			return s.toString();
		}
	}

	protected PersistentClass createClass() {
		if(getClass()==PersistentClass.class) {
			PersistentClass clazz=(PersistentClass)connection.create(new ClassClass(),new Class[] {Class.class},new Object[] {getClass()});
			clazz.setup();
			clazz.accessor().clazz=clazz;
			return clazz;
		} else return super.createClass();
	}

	void setup() {
		map=new HashMap();
		size=Field.HEADER_SIZE;
		String str=getFields();
		String fields[]=str.length()==0?new String[0]:str.split(";");
		for(int i=0;i<fields.length;i++) {
			Field f=new Field(fields[i]);
			f.setOffset(size);
			map.put(f.name, f);
			size+=f.size;
		}
		name=getName();
	}

	int size() {
		if(map==null) setup();
		return size;
	}

	Field getField(String name) {
		if(map==null) setup();
		Field f=(Field)map.get(name);
		if(f==null) throw new PersistentException("no such property : "+name+" in class : "+name());
		return f;
	}

	Iterator fieldIterator() {
		if(map==null) setup();
		return map.values().iterator();
	}

	public String name() {
		if(map==null) setup();
		return name;
	}

	public String getName() {
		return (String)get("name");
	}

	public void setName(String str) {
		set("name",str);
	}

	public String getFields() {
		return (String)get("fields");
	}

	public void setFields(String str) {
		set("fields",str);
	}

	static PersistentClass create(Class clazz, StoreImpl store) {
		PersistentObject obj=newInstance(clazz);
		obj.connection=store.systemConnection;
		return obj.createClass();
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
			return newInstance(Class.forName(name()));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
