package persistence.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import persistence.Array;
import persistence.PersistentClass;
import persistence.PersistentObject;

public class ObjectTableModel extends AbstractTableModel {
	Object object;
	String fields[];

	public ObjectTableModel(Object object) {
		this.object=object;
		init();
	}

	void init() {
		if(object instanceof PersistentObject) {
			PersistentObject obj=(PersistentObject)object;
			PersistentClass clazz=obj.persistentClass();
			String str=clazz.getFields();
			fields=str.length()==0?new String[0]:str.split(";");
		} else fields=new String[0];
	}

	void reload() {
		init();
		fireTableDataChanged();
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return fields.length;
	}

	public String getColumnName(int column) {
		switch(column) {
			case 0:
				return "field";
			case 1:
				return "value";
			default:
				return null;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0:
				return fields[rowIndex].substring(2);
			case 1:
				return get(rowIndex);
			default:
				return null;
		}
	}

	Object get(int n) {
		String f=fields[n];
		try {
			return object.getClass().getMethod((f.substring(0, 1).equals("Z")?"is":"get")+f.substring(2, 3).toUpperCase()+f.substring(3), new Class[] {}).invoke(object, new Object[] {});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static ObjectTableModel model(Object object) {
		if(object instanceof Array) return new ArrayTableModel((Array)object);
		else if(object instanceof Map) return new MapTableModel((Map)object);
		else if(object instanceof List) return new ListTableModel((List)object);
		else if(object instanceof Collection) return new CollectionTableModel((Collection)object);
		else return new ObjectTableModel(object);
	}
}

class ArrayTableModel extends ObjectTableModel {
	Array array;

	public ArrayTableModel(Array array) {
		super(array);
	}

	void init() {
		array=(Array)object;
	}

	public int getRowCount() {
		return array.length();
	}

	public String getColumnName(int column) {
		switch(column) {
			case 0:
				return "index";
			case 1:
				return "value";
			default:
				return null;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0:
				return new Integer(rowIndex);
			case 1:
				return get(rowIndex);
			default:
				return null;
		}
	}

	Object get(int n) {
		return array.get(n);
	}
}

class CollectionTableModel extends ObjectTableModel {
	List list;

	public CollectionTableModel(Collection collection) {
		super(collection);
	}

	void init() {
		list=new ArrayList((Collection)object);
	}

	public int getColumnCount() {
		return 1;
	}

	public int getRowCount() {
		return list.size();
	}

	public String getColumnName(int column) {
		switch(column) {
			case 0:
				return "element";
			default:
				return null;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0:
				return get(rowIndex);
			default:
				return null;
		}
	}

	Object get(int n) {
		return list.get(n);
	}
}

class ListTableModel extends CollectionTableModel {
	public ListTableModel(List list) {
		super(list);
	}

	void init() {
		list=(List)object;
	}

	public int getColumnCount() {
		return 2;
	}

	public String getColumnName(int column) {
		switch(column) {
			case 0:
				return "index";
			case 1:
				return "value";
			default:
				return null;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0:
				return new Integer(rowIndex);
			case 1:
				return get(rowIndex);
			default:
				return null;
		}
	}
}

class MapTableModel extends ObjectTableModel {
	Map map;
	Object keys[];

	public MapTableModel(Map map) {
		super(map);
	}

	void init() {
		map=(Map)object;
		keys=map.keySet().toArray();
	}

	public int getRowCount() {
		return keys.length;
	}

	public String getColumnName(int column) {
		switch(column) {
			case 0:
				return "key";
			case 1:
				return "value";
			default:
				return null;
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0:
				return keys[rowIndex];
			case 1:
				return get(rowIndex);
			default:
				return null;
		}
	}

	Object get(int n) {
		return map.get(keys[n]);
	}
}
