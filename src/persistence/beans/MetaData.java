/*
 * @(#)MetaData.java	1.33 05/04/29
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.EventHandler;
import java.beans.Introspector;
import java.beans.ConstructorProperties;

import persistence.PersistentArray;
import persistence.Store;

class NullPersistenceDelegate extends PersistenceDelegate {
	public NullPersistenceDelegate(Store store) {
		super(store);
	}

	// Note this will be called by all classes when they reach the
	// top of their superclass chain.
	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
	}
	protected Expression instantiate(Object oldInstance, Encoder out) { return null;}

	public void writeObject(Object oldInstance, Encoder out) {
	// System.out.println("NullPersistenceDelegate:writeObject " + oldInstance);
	}
}

class PrimitivePersistenceDelegate extends PersistenceDelegate {
	public PrimitivePersistenceDelegate(Store store) {
		super(store);
	}

	protected boolean mutatesTo(Object oldInstance, Object newInstance) {
		return oldInstance.equals(newInstance);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		return new Expression(store, oldInstance, oldInstance.getClass(),
				  "new", new Object[]{oldInstance.toString()});
	}
}

class ArrayPersistenceDelegate extends PersistenceDelegate {
	public ArrayPersistenceDelegate(Store store) {
		super(store);
		defaultPersistenceDelegate = new DefaultPersistenceDelegate(store);
	}

	DefaultPersistenceDelegate defaultPersistenceDelegate;

	protected boolean mutatesTo(Object oldInstance, Object newInstance) {
		return (newInstance != null &&
				oldInstance.getClass() == newInstance.getClass() && // Also ensures the subtype is correct.
				Array.getLength(oldInstance) == Array.getLength(newInstance));
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		// System.out.println("instantiate: " + type + " " + oldInstance);
		Class oldClass = oldInstance.getClass();
		return new Expression(store, oldInstance, Array.class, "newInstance",
				   new Object[]{oldClass.getComponentType(),
								new Integer(Array.getLength(oldInstance))});
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		int n = Array.getLength(oldInstance);
		for (int i = 0; i < n; i++) {
			Object index = new Integer(i);
			// Expression oldGetExp = new Expression(store, Array.class, "get", new Object[]{oldInstance, index});
			// Expression newGetExp = new Expression(store, Array.class, "get", new Object[]{newInstance, index});
			Expression oldGetExp = new Expression(store, oldInstance, "get", new Object[]{index});
			Expression newGetExp = new Expression(store, newInstance, "get", new Object[]{index});
			try {
				Object oldValue = oldGetExp.getValue();
				Object newValue = newGetExp.getValue();
				out.writeExpression(oldGetExp);
				if (!MetaData.equals(newValue, out.get(oldValue))) {
					// System.out.println("Not equal: " + newGetExp + " != " + actualGetExp);
					// invokeStatement(Array.class, "set", new Object[]{oldInstance, index, oldValue}, out);
					defaultPersistenceDelegate.invokeStatement(oldInstance, "set", new Object[]{index, oldValue}, out);
				}
			} catch (Exception e) {
				// System.err.println("Warning:: failed to write: " + oldGetExp);
				out.getExceptionListener().exceptionThrown(e);
			}
		}
	}
}

class persistence_PersistentArray_PersistenceDelegate extends DefaultPersistenceDelegate {
	public persistence_PersistentArray_PersistenceDelegate(Store store) {
		super(store);
	}

	protected boolean mutatesTo(Object oldInstance, Object newInstance) {
		persistence.Array oldO = (persistence.Array)oldInstance;
		persistence.Array newO = newInstance==null?null:(persistence.Array)newInstance;
		return (newO != null && oldO.typeCode() == newO.typeCode() && oldO.length() == newO.length());
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		persistence.Array oldO = (persistence.Array)oldInstance;
		return new Expression(store, oldInstance, PersistentArray.class, "newInstance", new Object[]{new Character(oldO.typeCode()), new Integer(oldO.length())});
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		persistence.Array oldO = (persistence.Array)oldInstance;
		persistence.Array newO = (persistence.Array)newInstance;
		int n = oldO.length();
		for (int i = 0; i < n; i++) {
			Object index = new Integer(i);

			Expression oldGetExp = new Expression(store, oldInstance, "get", new Object[]{index});
			Expression newGetExp = new Expression(store, newInstance, "get", new Object[]{index});
			try {
				Object oldValue = oldGetExp.getValue();
				Object newValue = newGetExp.getValue();
				out.writeExpression(oldGetExp);
				if (!MetaData.equals(newValue, out.get(oldValue))) {
					invokeStatement(oldInstance, "set", new Object[]{index, oldValue}, out);
				}
			} catch (Exception e) {
				out.getExceptionListener().exceptionThrown(e);
			}
		}
	}
}

class ProxyPersistenceDelegate extends PersistenceDelegate {
	public ProxyPersistenceDelegate(Store store) {
		super(store);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		Class type = oldInstance.getClass();
		java.lang.reflect.Proxy p = (java.lang.reflect.Proxy)oldInstance;
		// This unappealing hack is not required but makes the
		// representation of EventHandlers much more concise.
		java.lang.reflect.InvocationHandler ih = java.lang.reflect.Proxy.getInvocationHandler(p);
		if (ih instanceof EventHandler) {
			EventHandler eh = (EventHandler)ih;
			Vector args = new Vector();
			args.add(type.getInterfaces()[0]);
			args.add(eh.getTarget());
			args.add(eh.getAction());
			if (eh.getEventPropertyName() != null) {
				args.add(eh.getEventPropertyName());
			}
			if (eh.getListenerMethodName() != null) {
				args.setSize(4);
				args.add(eh.getListenerMethodName());
			}
			return new Expression(store, oldInstance, EventHandler.class, "create", args.toArray());
		}
		return new Expression(store, oldInstance, java.lang.reflect.Proxy.class, "newProxyInstance", new Object[]{type.getClassLoader(), type.getInterfaces(), ih});
	}
}

// Strings
class java_lang_String_PersistenceDelegate extends PersistenceDelegate {
	public java_lang_String_PersistenceDelegate(Store store) {
		super(store);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) { return null; }

	public void writeObject(Object oldInstance, Encoder out) {
		// System.out.println("NullPersistenceDelegate:writeObject " + oldInstance);
	}
}

// Classes
class java_lang_Class_PersistenceDelegate extends PersistenceDelegate {
	public java_lang_Class_PersistenceDelegate(Store store) {
		super(store);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		Class c = (Class)oldInstance;
		// As of 1.3 it is not possible to call Class.forName("int"),
		// so we have to generate different code for primitive types.
		// This is needed for arrays whose subtype may be primitive.
		if (c.isPrimitive()) {
			Field field = null;
			try {
				field = ReflectionUtils.typeToClass(c).getDeclaredField("TYPE");
			} catch (NoSuchFieldException ex) {
				System.err.println("Unknown primitive type: " + c);
			}
			return new Expression(store, oldInstance, field, "get", new Object[]{null});
		} else if (oldInstance == String.class) {
			return new Expression(store, oldInstance, "", "getClass", new Object[]{});
		} else if (oldInstance == Class.class) {
			return new Expression(store, oldInstance, String.class, "getClass", new Object[]{});
		} else {
			return new Expression(store, oldInstance, Class.class, "forName", new Object[]{c.getName()});
		}
	}
}

// Fields
class java_lang_reflect_Field_PersistenceDelegate extends PersistenceDelegate {
	public java_lang_reflect_Field_PersistenceDelegate(Store store) {
		super(store);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		Field f = (Field)oldInstance;
		return new Expression(store, oldInstance, f.getDeclaringClass(), "getField", new Object[]{f.getName()});
	}
}

// Methods
class java_lang_reflect_Method_PersistenceDelegate extends PersistenceDelegate {
	public java_lang_reflect_Method_PersistenceDelegate(Store store) {
		super(store);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		Method m = (Method)oldInstance;
		return new Expression(store, oldInstance, m.getDeclaringClass(), "getMethod", new Object[]{m.getName(), m.getParameterTypes()});
	}
}

// Collections

/*
The Hashtable and AbstractMap classes have no common ancestor yet may
be handled with a single persistence delegate: one which uses the methods
of the Map insterface exclusively. Attatching the persistence delegates
to the interfaces themselves is fraught however since, in the case of
the Map, both the AbstractMap and HashMap classes are declared to
implement the Map interface, leaving the obvious implementation prone
to repeating their initialization. These issues and questions around
the ordering of delegates attached to interfaces have lead us to
ignore any delegates attached to interfaces and force all persistence
delegates to be registered with concrete classes.
*/

// Collection
class java_util_Collection_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_util_Collection_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		java.util.Collection oldO = (java.util.Collection)oldInstance;
		java.util.Collection newO = (java.util.Collection)newInstance;

		if (newO.size() != 0) {
			invokeStatement(oldInstance, "clear", new Object[]{}, out);
		}
		for (Iterator i = oldO.iterator();i.hasNext();) {
			invokeStatement(oldInstance, "add", new Object[]{i.next()}, out);
		}
	}
}

// List
class java_util_List_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_util_List_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		java.util.List oldO = (java.util.List)oldInstance;
		java.util.List newO = (java.util.List)newInstance;
		int oldSize = oldO.size();
		int newSize = (newO == null) ? 0 : newO.size();
		if (oldSize < newSize) {
			invokeStatement(oldInstance, "clear", new Object[]{}, out);
			newSize = 0;
		}
		for (int i = 0;i < newSize;i++) {
			Object index = new Integer(i);

			Expression oldGetExp = new Expression(store, oldInstance, "get", new Object[]{index});
			Expression newGetExp = new Expression(store, newInstance, "get", new Object[]{index});
			try {
				Object oldValue = oldGetExp.getValue();
				Object newValue = newGetExp.getValue();
				out.writeExpression(oldGetExp);
				if (!MetaData.equals(newValue, out.get(oldValue))) {
					invokeStatement(oldInstance, "set", new Object[]{index, oldValue}, out);
				}
			} catch (Exception e) {
				out.getExceptionListener().exceptionThrown(e);
			}
		}
		for (int i = newSize;i < oldSize;i++) {
			invokeStatement(oldInstance, "add", new Object[]{oldO.get(i)}, out);
		}
	}
}

// Map
class java_util_Map_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_util_Map_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		// System.out.println("Initializing: " + newInstance);
		java.util.Map oldMap = (java.util.Map)oldInstance;
		java.util.Map newMap = (java.util.Map)newInstance;
		// Remove the new elements.
		// Do this first otherwise we undo the adding work.
		if (newMap != null) {
			java.util.Iterator newKeys = newMap.keySet().iterator();
			while(newKeys.hasNext()) {
				Object newKey = newKeys.next();
			   // PENDING: This "key" is not in the right environment.
				if (!oldMap.containsKey(newKey)) {
					invokeStatement(oldInstance, "remove", new Object[]{newKey}, out);
				}
			}
		}
		// Add the new elements.
		java.util.Iterator oldKeys = oldMap.keySet().iterator();
		while(oldKeys.hasNext()) {
			Object oldKey = oldKeys.next();

			Expression oldGetExp = new Expression(store, oldInstance, "get", new Object[]{oldKey});
			// Pending: should use newKey.
			Expression newGetExp = new Expression(store, newInstance, "get", new Object[]{oldKey});
			try {
				Object oldValue = oldGetExp.getValue();
				Object newValue = newGetExp.getValue();
				out.writeExpression(oldGetExp);
				if (!MetaData.equals(newValue, out.get(oldValue))) {
					invokeStatement(oldInstance, "put", new Object[]{oldKey, oldValue}, out);
				}
			} catch (Exception e) {
				out.getExceptionListener().exceptionThrown(e);
			}
		}
	}
}

class java_util_AbstractCollection_PersistenceDelegate extends java_util_Collection_PersistenceDelegate {
	public java_util_AbstractCollection_PersistenceDelegate(Store store) {
		super(store);
	}
}

class persistence_util_AbstractCollection_PersistenceDelegate extends java_util_Collection_PersistenceDelegate {
	public persistence_util_AbstractCollection_PersistenceDelegate(Store store) {
		super(store);
	}
}

class java_util_AbstractList_PersistenceDelegate extends java_util_List_PersistenceDelegate {
	public java_util_AbstractList_PersistenceDelegate(Store store) {
		super(store);
	}
}

class persistence_util_AbstractList_PersistenceDelegate extends java_util_List_PersistenceDelegate {
	public persistence_util_AbstractList_PersistenceDelegate(Store store) {
		super(store);
	}
}

class java_util_AbstractMap_PersistenceDelegate extends java_util_Map_PersistenceDelegate {
	public java_util_AbstractMap_PersistenceDelegate(Store store) {
		super(store);
	}
}

class java_util_Hashtable_PersistenceDelegate extends java_util_Map_PersistenceDelegate {
	public java_util_Hashtable_PersistenceDelegate(Store store) {
		super(store);
	}
}

class persistence_util_AbstractMap_PersistenceDelegate extends java_util_Map_PersistenceDelegate {
	public persistence_util_AbstractMap_PersistenceDelegate(Store store) {
		super(store);
	}
}

// Beans
class java_beans_beancontext_BeanContextSupport_PersistenceDelegate extends java_util_Collection_PersistenceDelegate {
	public java_beans_beancontext_BeanContextSupport_PersistenceDelegate(Store store) {
		super(store);
	}
}

// AWT

class StaticFieldsPersistenceDelegate extends PersistenceDelegate {
	public StaticFieldsPersistenceDelegate(Store store) {
		super(store);
	}

	protected void installFields(Encoder out, Class cls) {
		Field fields[] = cls.getFields();
		for(int i = 0;i < fields.length;i++) {
			Field field = fields[i];
			// Don't install primitives, their identity will not be preserved
			// by wrapping.
			if (Object.class.isAssignableFrom(field.getType())) {
				out.writeExpression(new Expression(store, field, "get", new Object[]{null}));
			}
		}
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		throw new RuntimeException("Unrecognized instance: " + oldInstance);
	}

	public void writeObject(Object oldInstance, Encoder out) {
		if (out.getAttribute(this) == null) {
			out.setAttribute(this, Boolean.TRUE);
			installFields(out, oldInstance.getClass());
		}
		super.writeObject(oldInstance, out);
	}
}

// SystemColor
class java_awt_SystemColor_PersistenceDelegate extends StaticFieldsPersistenceDelegate {
	public java_awt_SystemColor_PersistenceDelegate(Store store) {
		super(store);
	}
}

// TextAttribute
class java_awt_font_TextAttribute_PersistenceDelegate extends StaticFieldsPersistenceDelegate {
	public java_awt_font_TextAttribute_PersistenceDelegate(Store store) {
		super(store);
	}
}

// MenuShortcut
class java_awt_MenuShortcut_PersistenceDelegate extends PersistenceDelegate {
	public java_awt_MenuShortcut_PersistenceDelegate(Store store) {
		super(store);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		java.awt.MenuShortcut m = (java.awt.MenuShortcut)oldInstance;
		return new Expression(store, oldInstance, m.getClass(), "new", new Object[]{new Integer(m.getKey()), Boolean.valueOf(m.usesShiftModifier())});
	}
}

// Component
class java_awt_Component_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_Component_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		java.awt.Component c = (java.awt.Component)oldInstance;
		java.awt.Component c2 = (java.awt.Component)newInstance;
		// The "background", "foreground" and "font" properties.
		// The foreground and font properties of Windows change from
		// null to defined values after the Windows are made visible -
		// special case them for now.
		if (!(oldInstance instanceof java.awt.Window)) {
			String[] fieldNames = new String[]{"background", "foreground", "font"};
			for(int i = 0;i < fieldNames.length;i++) {
				String name = fieldNames[i];
				Object oldValue = ReflectionUtils.getPrivateField (oldInstance, java.awt.Component.class, name, out.getExceptionListener());
				Object newValue = (newInstance == null) ? null : ReflectionUtils.getPrivateField(newInstance, java.awt.Component.class, name, out.getExceptionListener());
				if (oldValue != null && !oldValue.equals(newValue)) {
					invokeStatement(oldInstance, "set" + NameGenerator.capitalize(name), new Object[]{oldValue}, out);
				}
			}
		}

		// Bounds
		java.awt.Container p = c.getParent();
		if (p == null || p.getLayout() == null && !(p instanceof javax.swing.JLayeredPane)) {
			// Use the most concise construct.
			boolean locationCorrect = c.getLocation().equals(c2.getLocation());
			boolean sizeCorrect = c.getSize().equals(c2.getSize());
			if (!locationCorrect && !sizeCorrect) {
				invokeStatement(oldInstance, "setBounds", new Object[]{c.getBounds()}, out);
			}
			else if (!locationCorrect) {
				invokeStatement(oldInstance, "setLocation", new Object[]{c.getLocation()}, out);
			}
			else if (!sizeCorrect) {
				invokeStatement(oldInstance, "setSize", new Object[]{c.getSize()}, out);
			}
		}
	}
}

// Container
class java_awt_Container_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_Container_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		// Ignore the children of a JScrollPane.
		// Pending(milne) find a better way to do this.
		if (oldInstance instanceof javax.swing.JScrollPane) {
			return;
		}
		java.awt.Container oldC = (java.awt.Container)oldInstance;
		java.awt.Component[] oldChildren = oldC.getComponents();
		java.awt.Container newC = (java.awt.Container)newInstance;
		java.awt.Component[] newChildren = (newC == null) ? new java.awt.Component[0] : newC.getComponents();
		// Pending. Assume all the new children are unaltered.
		for(int i = newChildren.length;i < oldChildren.length;i++) {
			invokeStatement(oldInstance, "add", new Object[]{oldChildren[i]}, out);
		}
	}
}

// Choice
class java_awt_Choice_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_Choice_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		java.awt.Choice m = (java.awt.Choice)oldInstance;
		java.awt.Choice n = (java.awt.Choice)newInstance;
		for (int i = n.getItemCount();i < m.getItemCount();i++) {
			invokeStatement(oldInstance, "add", new Object[]{m.getItem(i)}, out);
		}
	}
}

// Menu
class java_awt_Menu_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_Menu_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		java.awt.Menu m = (java.awt.Menu)oldInstance;
		java.awt.Menu n = (java.awt.Menu)newInstance;
		for (int i = n.getItemCount();i < m.getItemCount();i++) {
			invokeStatement(oldInstance, "add", new Object[]{m.getItem(i)}, out);
		}
	}
}

// MenuBar
class java_awt_MenuBar_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_MenuBar_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		java.awt.MenuBar m = (java.awt.MenuBar)oldInstance;
		java.awt.MenuBar n = (java.awt.MenuBar)newInstance;
		for (int i = n.getMenuCount();i < m.getMenuCount();i++) {
			invokeStatement(oldInstance, "add", new Object[]{m.getMenu(i)}, out);
		}
	}
}

// List
class java_awt_List_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_List_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		java.awt.List m = (java.awt.List)oldInstance;
		java.awt.List n = (java.awt.List)newInstance;
		for (int i = n.getItemCount();i < m.getItemCount();i++) {
			invokeStatement(oldInstance, "add", new Object[]{m.getItem(i)}, out);
		}
	}
}

// LayoutManagers

// BorderLayout
class java_awt_BorderLayout_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_BorderLayout_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {

		super.initialize(type, oldInstance, newInstance, out);
		String[] locations = {"north", "south", "east", "west", "center"};
		String[] names = {
			java.awt.BorderLayout.NORTH, java.awt.BorderLayout.SOUTH,
			java.awt.BorderLayout.EAST, java.awt.BorderLayout.WEST,
			java.awt.BorderLayout.CENTER
		};
		for(int i = 0;i < locations.length;i++) {
			Object oldC = ReflectionUtils.getPrivateField(oldInstance,
							   java.awt.BorderLayout.class,
							   locations[i], out.getExceptionListener());
			Object newC = ReflectionUtils.getPrivateField(newInstance,
							   java.awt.BorderLayout.class,
							   locations[i], out.getExceptionListener());
			// Pending, assume any existing elements are OK.
			if (oldC != null && newC == null) {
				invokeStatement(oldInstance, "addLayoutComponent", new Object[]{oldC, names[i]}, out);
			}
		}
	}
}

// CardLayout
class java_awt_CardLayout_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_CardLayout_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		Hashtable tab = (Hashtable)ReflectionUtils.getPrivateField(oldInstance, java.awt.CardLayout.class, "tab", out.getExceptionListener());
		if (tab != null) {
			for(Enumeration e = tab.keys();e.hasMoreElements();) {
				Object child = e.nextElement();
				invokeStatement(oldInstance, "addLayoutComponent", new Object[]{child, (String)tab.get(child)}, out);
			}
		}
	}
}

// GridBagLayout
class java_awt_GridBagLayout_PersistenceDelegate extends DefaultPersistenceDelegate {
	public java_awt_GridBagLayout_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		Hashtable comptable = (Hashtable)ReflectionUtils.getPrivateField (oldInstance, java.awt.GridBagLayout.class, "comptable", out.getExceptionListener());
		if (comptable != null) {
			for(Enumeration e = comptable.keys();e.hasMoreElements();) {
				Object child = e.nextElement();
				invokeStatement(oldInstance, "addLayoutComponent", new Object[]{child, comptable.get(child)}, out);
			}
		}
	}
}

// Swing

// JFrame (If we do this for Window instead of JFrame, the setVisible call
// will be issued before we have added all the children to the JFrame and
// will appear blank).
class javax_swing_JFrame_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_JFrame_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		java.awt.Window oldC = (java.awt.Window)oldInstance;
		java.awt.Window newC = (java.awt.Window)newInstance;
		boolean oldV = oldC.isVisible();
		boolean newV = newC.isVisible();
		if (newV != oldV) {
			// false means: don't execute this statement at write time.
			boolean executeStatements = out.executeStatements;
			out.executeStatements = false;
			invokeStatement(oldInstance, "setVisible", new Object[]{Boolean.valueOf(oldV)}, out);
			out.executeStatements = executeStatements;
		}
	}
}

// Models

// DefaultListModel
class javax_swing_DefaultListModel_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_DefaultListModel_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		// Note, the "size" property will be set here.
		super.initialize(type, oldInstance, newInstance, out);
		javax.swing.DefaultListModel m = (javax.swing.DefaultListModel)oldInstance;
		javax.swing.DefaultListModel n = (javax.swing.DefaultListModel)newInstance;
		for (int i = n.getSize();i < m.getSize();i++) {
			invokeStatement(oldInstance, "add", new Object[]{m.getElementAt(i)}, out); // Can also use "addElement".
		}
	}
}

// DefaultComboBoxModel
class javax_swing_DefaultComboBoxModel_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_DefaultComboBoxModel_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		javax.swing.DefaultComboBoxModel m = (javax.swing.DefaultComboBoxModel)oldInstance;
		for (int i = 0;i < m.getSize();i++) {
			invokeStatement(oldInstance, "addElement", new Object[]{m.getElementAt(i)}, out);
		}
	}
}

// DefaultMutableTreeNode
class javax_swing_tree_DefaultMutableTreeNode_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_tree_DefaultMutableTreeNode_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		javax.swing.tree.DefaultMutableTreeNode m = (javax.swing.tree.DefaultMutableTreeNode)oldInstance;
		javax.swing.tree.DefaultMutableTreeNode n = (javax.swing.tree.DefaultMutableTreeNode)newInstance;
		for (int i = n.getChildCount();i < m.getChildCount();i++) {
			invokeStatement(oldInstance, "add", new Object[]{m.getChildAt(i)}, out);
		}
	}
}

// ToolTipManager
class javax_swing_ToolTipManager_PersistenceDelegate extends PersistenceDelegate {
	public javax_swing_ToolTipManager_PersistenceDelegate(Store store) {
		super(store);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		return new Expression(store, oldInstance, javax.swing.ToolTipManager.class, "sharedInstance", new Object[]{});
	}
}

// JComponents

// JComponent (minimumSize, preferredSize & maximumSize).
// Note the "size" methods in JComponent calculate default values
// when their values are null. In Kestrel the new "isPreferredSizeSet"
// family of methods can be used to disambiguate this situation.
// We use the private fields here so that the code will work with
// Kestrel beta.
class javax_swing_JComponent_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_JComponent_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		int statementCount = 0;
		javax.swing.JComponent c = (javax.swing.JComponent)oldInstance;
		String[] fieldNames = new String[]{"minimumSize", "preferredSize", "maximumSize"};
		for(int i = 0;i < fieldNames.length;i++) {
			String name = fieldNames[i];
			Object value = ReflectionUtils.getPrivateField (c, javax.swing.JComponent.class, name, out.getExceptionListener());

			if (value != null) {
				// System.out.println("Setting " + name);
				invokeStatement(oldInstance, "set" + NameGenerator.capitalize(name), new Object[]{value}, out);
			}
		}
	}
}

// JTabbedPane
class javax_swing_JTabbedPane_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_JTabbedPane_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		javax.swing.JTabbedPane p = (javax.swing.JTabbedPane)oldInstance;
		for (int i = 0;i < p.getTabCount();i++) {
			invokeStatement(oldInstance, "addTab", new Object[] {p.getTitleAt(i), p.getIconAt(i), p.getComponentAt(i)}, out);
		}
	}
}

// JMenu
// Note that we do not need to state the initialiser for
// JMenuItems since the getComponents() method defined in
// Container will return all of the sub menu items that
// need to be added to the menu item.
// Not so for JMenu apparently.
class javax_swing_JMenu_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_JMenu_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		javax.swing.JMenu m = (javax.swing.JMenu)oldInstance;
		java.awt.Component[] c = m.getMenuComponents();
		for (int i = 0;i < c.length;i++) {
			invokeStatement(oldInstance, "add", new Object[] {c[i]}, out);
		}
	}
}

/* XXX - doesn't seem to work. Debug later.
class javax_swing_JMenu_PersistenceDelegate extends DefaultPersistenceDelegate {
	public javax_swing_JMenu_PersistenceDelegate(Store store) {
		super(store);
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		super.initialize(type, oldInstance, newInstance, out);
		javax.swing.JMenu m = (javax.swing.JMenu)oldInstance;
		javax.swing.JMenu n = (javax.swing.JMenu)newInstance;
		for (int i = n.getItemCount();i < m.getItemCount();i++) {
			invokeStatement(oldInstance, "add", new Object[] {m.getItem(i)}, out);
		}
	}
}
*/

class MetaData {
	Store store;
	private Hashtable internalPersistenceDelegates = new Hashtable();
	private Hashtable transientProperties = new Hashtable();

	private PersistenceDelegate nullPersistenceDelegate;
	private PersistenceDelegate primitivePersistenceDelegate;
	private PersistenceDelegate defaultPersistenceDelegate;
	private PersistenceDelegate arrayPersistenceDelegate;
	private PersistenceDelegate proxyPersistenceDelegate;

	MetaData(Store store) {
		this.store=store;
		nullPersistenceDelegate = new NullPersistenceDelegate(store);
		primitivePersistenceDelegate = new PrimitivePersistenceDelegate(store);
		defaultPersistenceDelegate = new DefaultPersistenceDelegate(store);

// Constructors.

  // util

		registerConstructor("java.util.Date", new String[]{"time"});

  // beans

		registerConstructor("java.beans.Statement", new String[] {"target", "methodName", "arguments"});
		registerConstructor("java.beans.Expression", new String[] {"target", "methodName", "arguments"});
		registerConstructor("java.beans.EventHandler", new String[] {"target", "action", "eventPropertyName", "listenerMethodName"});

  // awt

		registerConstructor("java.awt.Point", new String[]{"x", "y"});
		registerConstructor("java.awt.Dimension", new String[]{"width", "height"});
		registerConstructor("java.awt.Rectangle", new String[]{"x", "y", "width", "height"});

		registerConstructor("java.awt.Insets", new String[]{"top", "left", "bottom", "right"});
		registerConstructor("java.awt.Color", new String[]{"red", "green", "blue", "alpha"});
		registerConstructor("java.awt.Font", new String[]{"name", "style", "size"});
		registerConstructor("java.awt.Cursor", new String[]{"type"});
		registerConstructor("java.awt.GridBagConstraints", new String[] {"gridx", "gridy", "gridwidth", "gridheight", "weightx", "weighty", "anchor", "fill", "insets", "ipadx", "ipady"});
		registerConstructor("java.awt.ScrollPane", new String[]{"scrollbarDisplayPolicy"});

  // swing

		registerConstructor("javax.swing.plaf.FontUIResource", new String[]{"name", "style", "size"});
		registerConstructor("javax.swing.plaf.ColorUIResource", new String[]{"red", "green", "blue"});

		// registerConstructor(javax.swing.tree.DefaultTreeModel", new String[]{"root"});
		registerConstructor("javax.swing.tree.TreePath", new String[]{"path"});

		registerConstructor("javax.swing.OverlayLayout", new String[]{"target"});
		registerConstructor("javax.swing.BoxLayout", new String[]{"target", "axis"});
		registerConstructor("javax.swing.DefaultCellEditor", new String[]{"component"});

		/*
		This is required because the JSplitPane reveals a private layout class
		called BasicSplitPaneUI$BasicVerticalLayoutManager which changes with
		the orientation. To avoid the necessity for instantiating it we cause
		the orientation attribute to get set before the layout manager - that
		way the layout manager will be changed as a side effect. Unfortunately,
		the layout property belongs to the superclass and therefore precedes
		the orientation property. PENDING - we need to allow this kind of
		modification. For now, put the property in the constructor.
		*/
		registerConstructor("javax.swing.JSplitPane", new String[]{"orientation"});
		// Try to synthesize the ImageIcon from its description.
		registerConstructor("javax.swing.ImageIcon", new String[]{"description"});
		// JButton's "label" and "actionCommand" properties are related,
		// use the label as a constructor argument to ensure that it is set first.
		// This remove the benign, but unnecessary, manipulation of actionCommand
		// property in the common case.
		registerConstructor("javax.swing.JButton", new String[]{"label"});

		// borders

		registerConstructor("javax.swing.border.BevelBorder", new String[]{"bevelType", "highlightOuter", "highlightInner", "shadowOuter", "shadowInner"});
		registerConstructor("javax.swing.plaf.BorderUIResource$BevelBorderUIResource", new String[]{"bevelType", "highlightOuter", "highlightInner", "shadowOuter", "shadowInner"});
		registerConstructor("javax.swing.border.CompoundBorder", new String[]{"outsideBorder", "insideBorder"});
		registerConstructor("javax.swing.plaf.BorderUIResource$CompoundBorderUIResource", new String[]{"outsideBorder", "insideBorder"});
		registerConstructor("javax.swing.border.EmptyBorder", new String[]{"top", "left", "bottom", "right"});
		registerConstructor("javax.swing.plaf.BorderUIResource$EmptyBorderUIResource", new String[]{"top", "left", "bottom", "right"});
		registerConstructor("javax.swing.border.EtchedBorder", new String[]{"etchType", "highlight", "shadow"});
		registerConstructor("javax.swing.plaf.BorderUIResource$EtchedBorderUIResource", new String[]{"etchType", "highlight", "shadow"});
		registerConstructor("javax.swing.border.LineBorder", new String[]{"lineColor", "thickness"});
		registerConstructor("javax.swing.plaf.BorderUIResource$LineBorderUIResource", new String[]{"lineColor", "thickness"});
		// Note this should check to see which of "color" and "tileIcon" is non-null.
		registerConstructor("javax.swing.border.MatteBorder", new String[]{"top", "left", "bottom", "right", "tileIcon"});
		registerConstructor("javax.swing.plaf.BorderUIResource$MatteBorderUIResource", new String[]{"top", "left", "bottom", "right", "tileIcon"});
		registerConstructor("javax.swing.border.SoftBevelBorder", new String[]{"bevelType", "highlightOuter", "highlightInner", "shadowOuter", "shadowInner"});
		// registerConstructorWithBadEqual("javax.swing.plaf.BorderUIResource$SoftBevelBorderUIResource", new String[]{"bevelType", "highlightOuter", "highlightInner", "shadowOuter", "shadowInner"});
		registerConstructor("javax.swing.border.TitledBorder", new String[]{"border", "title", "titleJustification", "titlePosition", "titleFont", "titleColor"});
		registerConstructor("javax.swing.plaf.BorderUIResource$TitledBorderUIResource", new String[]{"border", "title", "titleJustification", "titlePosition", "titleFont", "titleColor"});

// Transient properties

  // awt

	// Infinite graphs.
		removeProperty("java.awt.geom.RectangularShape", "frame");
		// removeProperty("java.awt.Rectangle2D", "frame");
		// removeProperty("java.awt.Rectangle", "frame");

		removeProperty("java.awt.Rectangle", "bounds");
		removeProperty("java.awt.Dimension", "size");
		removeProperty("java.awt.Point", "location");

		// The color and font properties in Component need special treatment, see above.
		removeProperty("java.awt.Component", "foreground");
		removeProperty("java.awt.Component", "background");
		removeProperty("java.awt.Component", "font");

		// The visible property of Component needs special treatment because of Windows.
		removeProperty("java.awt.Component", "visible");

		// This property throws an exception if accessed when there is no child.
		removeProperty("java.awt.ScrollPane", "scrollPosition");

  // swing

		// The size properties in JComponent need special treatment, see above.
		removeProperty("javax.swing.JComponent", "minimumSize");
		removeProperty("javax.swing.JComponent", "preferredSize");
		removeProperty("javax.swing.JComponent", "maximumSize");

		// These properties have platform specific implementations
		// and should not appear in archives.
		removeProperty("javax.swing.ImageIcon", "image");
		removeProperty("javax.swing.ImageIcon", "imageObserver");

		// This property throws an exception when set in JMenu.
		// PENDING: Note we must delete the property from
		// the superclass even though the superclass's
		// implementation does not throw an error.
		// This needs some more thought.
		removeProperty("javax.swing.JMenu", "accelerator");
		removeProperty("javax.swing.JMenuItem", "accelerator");
		// This property unconditionally throws a "not implemented" exception.
		removeProperty("javax.swing.JMenuBar", "helpMenu");

		// The scrollBars in a JScrollPane are dynamic and should not
		// be archived. The row and columns headers are changed by
		// components like JTable on "addNotify".
		removeProperty("javax.swing.JScrollPane", "verticalScrollBar");
		removeProperty("javax.swing.JScrollPane", "horizontalScrollBar");
		removeProperty("javax.swing.JScrollPane", "rowHeader");
		removeProperty("javax.swing.JScrollPane", "columnHeader");

		removeProperty("javax.swing.JViewport", "extentSize");

		// Renderers need special treatment, since their properties
		// change during rendering.
		removeProperty("javax.swing.table.JTableHeader", "defaultRenderer");
		removeProperty("javax.swing.JList", "cellRenderer");

		removeProperty("javax.swing.JList", "selectedIndices");

		// The lead and anchor selection indexes are best ignored.
		// Selection is rarely something that should persist from
		// development to deployment.
		removeProperty("javax.swing.DefaultListSelectionModel", "leadSelectionIndex");
		removeProperty("javax.swing.DefaultListSelectionModel", "anchorSelectionIndex");

		// The selection must come after the text itself.
		removeProperty("javax.swing.JComboBox", "selectedIndex");

		// All selection information should come after the JTabbedPane is built
		removeProperty("javax.swing.JTabbedPane", "selectedIndex");
		removeProperty("javax.swing.JTabbedPane", "selectedComponent");

		// PENDING: The "disabledIcon" property is often computed from the icon property.
		removeProperty("javax.swing.AbstractButton", "disabledIcon");
		removeProperty("javax.swing.JLabel", "disabledIcon");

		// The caret property throws errors when it it set beyond
		// the extent of the text. We could just set it after the
		// text, but this is probably not something we want to archive anyway.
		removeProperty("javax.swing.text.JTextComponent", "caret");
		removeProperty("javax.swing.text.JTextComponent", "caretPosition");
		// The selectionStart must come after the text itself.
		removeProperty("javax.swing.text.JTextComponent", "selectionStart");
		removeProperty("javax.swing.text.JTextComponent", "selectionEnd");
	}

	/*pp*/ static boolean equals(Object o1, Object o2) {
		return (o1 == null) ? (o2 == null) : o1.equals(o2);
	}

	// Entry points for Encoder.

	public static synchronized void setPersistenceDelegate(Class type, PersistenceDelegate persistenceDelegate) {
		setBeanAttribute(type, "persistenceDelegate", persistenceDelegate);
	}

	public synchronized PersistenceDelegate getPersistenceDelegate(Class type) {
		if (type == null) {
			return nullPersistenceDelegate;
		}
		if (ReflectionUtils.isPrimitive(type)) {
			return primitivePersistenceDelegate;
		}
		// The persistence delegate for arrays is non-trivial;instantiate it lazily.
		if (type.isArray()) {
			if (arrayPersistenceDelegate == null) {
				arrayPersistenceDelegate = new ArrayPersistenceDelegate(store);
			}
			return arrayPersistenceDelegate;
		}
		// Handle proxies lazily for backward compatibility with 1.2.
		try {
			if (java.lang.reflect.Proxy.isProxyClass(type)) {
				if (proxyPersistenceDelegate == null) {
					proxyPersistenceDelegate = new ProxyPersistenceDelegate(store);
				}
				return proxyPersistenceDelegate;
			}
		} catch(Exception e) {}
		// else if (type.getDeclaringClass() != null) {
		//	 return new DefaultPersistenceDelegate(store, new String[]{"this$0"});
		// }

		String typeName = type.getName();

		// Check to see if there are properties that have been lazily registered for removal.
		if (getBeanAttribute(type, "transient_init") == null) {
			Vector tp = (Vector)transientProperties.get(typeName);
			if (tp != null) {
				for(int i = 0;i < tp.size();i++) {
					setPropertyAttribute(type, (String)tp.get(i), "transient", Boolean.TRUE);
				}
			}
			setBeanAttribute(type, "transient_init", Boolean.TRUE);
		}

		PersistenceDelegate pd = (PersistenceDelegate)getBeanAttribute(type, "persistenceDelegate");
		if (pd == null) {
			pd = (PersistenceDelegate)internalPersistenceDelegates.get(typeName);
			if (pd != null) {
				return pd;
			}
			internalPersistenceDelegates.put (typeName, defaultPersistenceDelegate);
			try {
				String name =  type.getName();
				Class c = Class.forName("persistence.beans." + name.replace('.', '_') + "_PersistenceDelegate");
				pd = (PersistenceDelegate)c.getConstructor(new Class[] {Store.class}).newInstance(new Object[] {store});
				internalPersistenceDelegates.put(typeName, pd);
			} catch (ClassNotFoundException e) {
				String[] properties = getConstructorProperties(type);
				if (properties != null) {
					pd = new DefaultPersistenceDelegate(store, properties);
					internalPersistenceDelegates.put(typeName, pd);
				}
			} catch (Exception e) {
				System.err.println("Internal error: " + e);
			}
		}

		return (pd != null) ? pd : defaultPersistenceDelegate;
	}

	private static String[] getConstructorProperties(Class type) {
		String[] names = null;
		int length = 0;
		for (Constructor<?> constructor : type.getConstructors()) {
			String[] value = getAnnotationValue(constructor);
			if ((value != null) && (length < value.length) && isValid(constructor, value)) {
				names = value;
				length = value.length;
			}
		}
		return names;
	}

	private static String[] getAnnotationValue(Constructor<?> constructor) {
		ConstructorProperties annotation = ((ConstructorProperties) constructor.getAnnotation(ConstructorProperties.class));
		return (annotation != null)
				? annotation.value()
				: null;
	}

	private static boolean isValid(Constructor<?> constructor, String[] names) {
		Class[] parameters = constructor.getParameterTypes();
		if (1 + names.length != parameters.length) {
			return false;
		}
		for (String name : names) {
			if (name == null) {
				return false;
			}
		}
		return true;
	}

	// Wrapper for Introspector.getBeanInfo to handle exception handling.
	// Note: this relys on new 1.4 Introspector semantics which cache the BeanInfos
	public static BeanInfo getBeanInfo(Class type) {
		BeanInfo info = null;
		try {
			info = Introspector.getBeanInfo(type);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return info;
	}

	private static PropertyDescriptor getPropertyDescriptor(Class type, String propertyName) {
		BeanInfo info = getBeanInfo(type);
		PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
		// System.out.println("Searching for: " + propertyName + " in " + type);
		for(int i = 0;i < propertyDescriptors.length;i++) {
			PropertyDescriptor pd  = propertyDescriptors[i];
			if (propertyName.equals(pd.getName())) {
				return pd;
			}
		}
		return null;
	}

	private static void setPropertyAttribute(Class type, String property, String attribute, Object value) {
		PropertyDescriptor pd = getPropertyDescriptor(type, property);
		if (pd == null) {
			System.err.println("Warning: property " + property + " is not defined on " + type);
			return;
		}
		pd.setValue(attribute, value);
	}

	private static void setBeanAttribute(Class type, String attribute, Object value) {
		getBeanInfo(type).getBeanDescriptor().setValue(attribute, value);
	}

	private static Object getBeanAttribute(Class type, String attribute) {
		return getBeanInfo(type).getBeanDescriptor().getValue(attribute);
	}

// MetaData registration

	private synchronized void registerConstructor(String typeName, String[] constructor) {
		internalPersistenceDelegates.put(typeName, new DefaultPersistenceDelegate(store, constructor));
	}

	private void removeProperty(String typeName, String property) {
		Vector tp = (Vector)transientProperties.get(typeName);
		if (tp == null) {
			tp = new Vector();
			transientProperties.put(typeName, tp);
		}
		tp.add(property);
	}
}
