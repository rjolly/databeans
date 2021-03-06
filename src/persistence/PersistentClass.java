package persistence;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PersistentClass extends PersistentObject {
	transient Map<String, Field> map;
	transient int size;
	transient String name;

	public PersistentClass() {
	}

	public PersistentClass(final PersistentObject obj) {
		this(obj.store, obj.getClass());
	}

	PersistentClass(final Store store, final Class<? extends PersistentObject> clazz) {
		super(store, clazz == PersistentClass.class?new ClassClass():null);
		init(clazz);
		if (clazz == PersistentClass.class) {
			setup();
			setClass(this);
		}
	}

	protected void init(Class<? extends PersistentObject> clazz) {
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
			if(d.getReadMethod().isAnnotationPresent(Secondary.class)) continue;
			buffer.append(first?"":";").append(new Field(d));
			first=false;
		}
		setFields(buffer.toString());
		setName(clazz.getName());
	}

	void setClass(final PersistentClass clazz) {
		this.clazz = clazz;
	}

	void setup() {
		map=new LinkedHashMap<>();
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
		if(f==null) throw new RuntimeException("no such property : "+name+" in class : "+name());
		return f;
	}

	Iterator<Field> fieldIterator() {
		if(map==null) setup();
		return map.values().iterator();
	}

	public String name() {
		if(map==null) setup();
		return name;
	}

	public String getName() {
		return get("name");
	}

	public void setName(String str) {
		set("name",str);
	}

	public String getFields() {
		return get("fields");
	}

	public void setFields(String str) {
		set("fields",str);
	}

	public String toString() {
		StringBuffer s=new StringBuffer();
		String fields[]=getFields().split(";");
		s.append(Long.toHexString(base));
		s.append("[");
		for(int i=0;i<fields.length;i++) s.append((i==0?"":", ")+fields[i]);
		s.append("]");
		return s.toString();
	}

	static PersistentObject newInstance(Class<? extends PersistentObject> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	PersistentObject newInstance() {
		try {
			return newInstance((Class<? extends PersistentObject>)Class.forName(name()));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
