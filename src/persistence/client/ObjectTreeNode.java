package persistence.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.tree.TreeNode;
import persistence.Array;
import persistence.PersistentClass;
import persistence.PersistentObject;

public class ObjectTreeNode implements TreeNode {
	Object object;
	String name;
	TreeNode parent;
	String fields[];

	public ObjectTreeNode(Object object, String name, TreeNode parent) {
		this.object=object;
		this.name=name;
		this.parent=parent;
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

	public Enumeration children() {
		return new Enumeration() {
			int index;

			public boolean hasMoreElements() {
				return index<fields.length;
			}

			public Object nextElement() {
				return get(index++);
			}
		};
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

	public boolean getAllowsChildren() {
		return false;
	}

	public TreeNode getChildAt(int childIndex) {
		return ObjectTreeNode.node(get(childIndex),fields[childIndex].substring(2),this);
	}

	public int getChildCount() {
		return fields.length;
	}

	public int getIndex(TreeNode node) {
		for(int i=0;i<fields.length;i++) if(fields[i].substring(2).equals(((ObjectTreeNode)node).name)) return i;
		return -1;
	}

	public TreeNode getParent() {
		return parent;
	}

	public boolean isLeaf() {
		return fields.length==0;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ObjectTreeNode other = (ObjectTreeNode) obj;
		if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}

	public String toString() {
		return name;
	}

	static ObjectTreeNode node(Object object, String name) {
		return node(object,name,null);
	}

	static ObjectTreeNode node(Object object, String name, TreeNode parent) {
		if(object instanceof Array) return new ArrayTreeNode((Array)object,name,parent);
		else if(object instanceof Map) return new MapTreeNode((Map)object,name,parent);
		else if(object instanceof List) return new ListTreeNode((List)object,name,parent);
		else if(object instanceof Collection) return new CollectionTreeNode((Collection)object,name,parent);
		else return new ObjectTreeNode(object,name,parent);
	}
}

class ArrayTreeNode extends ObjectTreeNode {
	Array array;

	public ArrayTreeNode(Array array, String name, TreeNode parent) {
		super(array,name,parent);
	}

	void init() {
		array=(Array)object;
	}

	public Enumeration children() {
		return new Enumeration() {
			int index;

			public boolean hasMoreElements() {
				return index<array.length();
			}

			public Object nextElement() {
				return get(index++);
			}
		};
	}

	Object get(int n) {
		return array.get(n);
	}

	public TreeNode getChildAt(int childIndex) {
		return ObjectTreeNode.node(get(childIndex),String.valueOf(childIndex),this);
	}

	public int getChildCount() {
		return array.length();
	}

	public int getIndex(TreeNode node) {
		return Integer.valueOf(((ObjectTreeNode)node).name).intValue();
	}

	public boolean isLeaf() {
		return array.length()==0;
	}
}

class CollectionTreeNode extends ObjectTreeNode {
	List list;

	public CollectionTreeNode(Collection collection, String name, TreeNode parent) {
		super(collection,name,parent);
	}

	void init() {
		list=new ArrayList((Collection)object);
	}

	public Enumeration children() {
		return new Enumeration() {
			Iterator iterator=list.iterator();

			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			public Object nextElement() {
				return iterator.next();
			}
		};
	}

	Object get(int n) {
		return list.get(n);
	}

	public TreeNode getChildAt(int childIndex) {
		return ObjectTreeNode.node(get(childIndex),String.valueOf(childIndex),this);
	}

	public int getChildCount() {
		return list.size();
	}

	public int getIndex(TreeNode node) {
		return Integer.valueOf(((ObjectTreeNode)node).name).intValue();
	}

	public boolean isLeaf() {
		return list.isEmpty();
	}
}

class ListTreeNode extends CollectionTreeNode {
	public ListTreeNode(List list, String name, TreeNode parent) {
		super(list,name,parent);
	}

	void init() {
		list=(List)object;
	}
}

class MapTreeNode extends ObjectTreeNode {
	Map map;
	Object keys[];

	public MapTreeNode(Map map, String name, TreeNode parent) {
		super(map,name,parent);
	}

	void init() {
		map=(Map)object;
		keys=map.keySet().toArray();
	}

	public Enumeration children() {
		return new Enumeration() {
			Iterator iterator=map.values().iterator();

			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			public Object nextElement() {
				return iterator.next();
			}
		};
	}

	Object get(int n) {
		return map.get(keys[n]);
	}

	public boolean getAllowsChildren() {
		return false;
	}

	public TreeNode getChildAt(int childIndex) {
		return ObjectTreeNode.node(get(childIndex),keys[childIndex].toString(),this);
	}

	public int getChildCount() {
		return keys.length;
	}

	public int getIndex(TreeNode node) {
		for(int i=0;i<keys.length;i++) if(keys[i].toString().equals(((ObjectTreeNode)node).name)) return i;
		return -1;
	}

	public boolean isLeaf() {
		return keys.length==0;
	}
}
