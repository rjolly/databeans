package persistence;

import java.io.*;
import java.util.*;
import java.beans.*;
import java.lang.reflect.*;

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
			throw new PersistentException("introspection error");
		}
		PropertyDescriptor desc[]=info.getPropertyDescriptors();
		Collection c=new ArrayList();
		for(int i=0;i<desc.length;i++) {
			PropertyDescriptor d=desc[i];
			if(d instanceof IndexedPropertyDescriptor) continue;
			if(d.getName().equals("class")) continue;
			if(d.getName().equals("ref")) continue;
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
		return (Field)map.get(name);
	}

	Iterator fieldIterator() {
		return map.values().iterator();
	}

	Class javaClass() {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new PersistentException("class not found");
		}
	}

	PersistentObject newInstance(Accessor accessor, Connection connection, Class types[], Object args[]) {
		Class clazz=javaClass();
		try {
			Class t[]=new Class[2+types.length];
			Object a[]=new Object[2+args.length];
			System.arraycopy(new Class[] {Accessor.class, Connection.class},0,t,0,2);
			System.arraycopy(types,0,t,2,types.length);
			System.arraycopy(new Object[] {accessor, connection},0,a,0,2);
			System.arraycopy(args,0,a,2,args.length);
			return (PersistentObject)clazz.getConstructor(t).newInstance(a);
		} catch (NoSuchMethodException e) {
			if(types.length==0 && args.length==0) return newInstance(accessor, connection);
			else throw new PersistentException("instantiation error");
		} catch (InstantiationException e) {
			throw new PersistentException("instantiation error");
		} catch (IllegalAccessException e) {
			throw new PersistentException("instantiation error");
		} catch (InvocationTargetException e) {
			throw new PersistentException("instantiation error");
		}
	}

	PersistentObject newInstance(Accessor accessor, Connection connection) {
		PersistentObject obj;
		Class clazz=javaClass();
		try {
			obj=(PersistentObject)clazz.newInstance();
		} catch (InstantiationException e) {
			throw new PersistentException("instantiation error");
		} catch (IllegalAccessException e) {
			throw new PersistentException("instantiation error");
		}
		obj.init(accessor,connection);
		return obj;
	}

	public String toString() {
		StringBuffer s=new StringBuffer();
		s.append(Long.toHexString(base));
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