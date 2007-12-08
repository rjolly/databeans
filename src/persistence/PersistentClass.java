package persistence;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PersistentClass extends UnicastSerializedObject {
	String name;
	Field fields[];
	transient Map map;
	transient int size;

	PersistentClass(Class clazz) {
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
		c.toArray(fields=new Field[c.size()]);
		name=clazz.getName();
		init();
	}

	private void init() {
		size+=Field.HEADER_SIZE;
		map=new HashMap();
		for(int i=0;i<fields.length;i++) {
			Field f=fields[i];
			f.setOffset(size);
			map.put(f.name, f);
			size+=f.size;
		}
	}

	Field getField(String name) {
		Field f=(Field)map.get(name);
		if(f==null) throw new PersistentException("no such property : "+name+" in class : "+this.name);
		return f;
	}

	Iterator fieldIterator() {
		return map.values().iterator();
	}

	PersistentObject newInstance() {
		try {
			return (PersistentObject)Class.forName(name).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		return name;
	}

	public String dump() {
		StringBuffer s=new StringBuffer();
		s.append("[");
		for(int i=0;i<fields.length;i++) s.append((i==0?"":", ")+fields[i]);
		s.append("]");
		return s.toString();
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
	}

	private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
		s.defaultReadObject();
		init();
	}
}
