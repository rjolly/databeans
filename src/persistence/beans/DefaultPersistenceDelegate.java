/*
 * @(#)DefaultPersistenceDelegate.java	1.15 05/08/30
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EventListener;
import persistence.Store;

public class DefaultPersistenceDelegate extends PersistenceDelegate {
	private String[] constructor;
	private Boolean definesEquals;

	public DefaultPersistenceDelegate(Store store) {
		this(store,new String[0]);
	}

	public DefaultPersistenceDelegate(Store store, String[] constructorPropertyNames) {
		super(store);
		this.constructor = constructorPropertyNames;
	}

	private static boolean definesEquals(Class type) {
		try {
			type.getDeclaredMethod("equals", new Class[]{Object.class});
			return true;
		} catch(NoSuchMethodException e) {
			return false;
		}
	}

	private boolean definesEquals(Object instance) {
		if (definesEquals != null) {
			return (definesEquals == Boolean.TRUE);
		} else {
			boolean result = definesEquals(instance.getClass());
			definesEquals = result ? Boolean.TRUE : Boolean.FALSE;
			return result;
		}
	}

	protected boolean mutatesTo(Object oldInstance, Object newInstance) {
		// Assume the instance is either mutable or a singleton
		// if it has a nullary constructor.
		return (constructor.length == 0) || !definesEquals(oldInstance) ?
			super.mutatesTo(oldInstance, newInstance) :
			oldInstance.equals(newInstance);
	}

	private static String capitalize(String propertyName) {
		return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		int nArgs = constructor.length;
		Class type = oldInstance.getClass();
		// System.out.println("writeObject: " + oldInstance);
		Object[] constructorArgs = new Object[nArgs];
		for(int i = 0; i < nArgs; i++) {
			/*
			1.2 introduces "public double getX()" et al. which return values
			which cannot be used in the constructors (they are the wrong type).
			In constructors, use public fields in preference to getters
			when they are defined.
			*/
			String name = constructor[i];

			Field f = null;
			try {
				// System.out.println("Trying field " + name + " in " + type);
				f = type.getDeclaredField(name);
				f.setAccessible(true);
			} catch (NoSuchFieldException e) {}
			try {
				constructorArgs[i] = (f != null && !Modifier.isStatic(f.getModifiers())) ?
				f.get(oldInstance) :
				MethodUtil.invoke(ReflectionUtils.getPublicMethod(type, "get" + NameGenerator.capitalize(name), new Class[0]), oldInstance, new Object[0]);
			} catch (Exception e) {
				// handleError(e, "Warning: Failed to get " + name + " property for " + oldInstance.getClass() + " constructor");
				out.getExceptionListener().exceptionThrown(e);
			}
		}
		return new Expression(store, oldInstance, oldInstance.getClass(), "new", constructorArgs);
	}

	// This is a workaround for a bug in the introspector.
	// PropertyDescriptors are not shared amongst subclasses.
	private boolean isTransient(Class type, PropertyDescriptor pd) {
		if (type == null) {
			return false;
		}
		// This code was mistakenly deleted - it may be fine and
		// is more efficient than the code below. This should
		// all disappear anyway when property descriptors are shared
		// by the introspector.
		/*
		Method getter = pd.getReadMethod();
		Class declaringClass = getter.getDeclaringClass();
		if (declaringClass == type) {
			return Boolean.TRUE.equals(pd.getValue("transient"));
		}
		*/
		String pName = pd.getName();
		BeanInfo info = MetaData.getBeanInfo(type);
		PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
		for (int i = 0; i < propertyDescriptors.length; ++i ) {
			PropertyDescriptor pd2 = propertyDescriptors[i];
			if (pName.equals(pd2.getName())) {
				Object value = pd2.getValue("transient");
				if (value != null) {
					return Boolean.TRUE.equals(value);
				}
			}
		}
		return isTransient(type.getSuperclass(), pd);
	}

	private static boolean equals(Object o1, Object o2) {
		return (o1 == null) ? (o2 == null) : o1.equals(o2);
	}

	private void doProperty(Class type, PropertyDescriptor pd, Object oldInstance, Object newInstance, Encoder out) throws Exception {
		Method getter = pd.getReadMethod();
		Method setter = pd.getWriteMethod();

		if (getter != null && setter != null && !isTransient(type, pd)) {
			Expression oldGetExp = new Expression(store, oldInstance, getter.getName(), new Object[]{});
			Expression newGetExp = new Expression(store, newInstance, getter.getName(), new Object[]{});
			Object oldValue = oldGetExp.getValue();
			Object newValue = newGetExp.getValue();
			out.writeExpression(oldGetExp);
			if (!equals(newValue, out.get(oldValue))) {
				// Search for a static constant with this value;
				Object e = (Object[])pd.getValue("enumerationValues");
				if (e instanceof Object[] && Array.getLength(e) % 3 == 0) {
					Object[] a = (Object[])e;
					for(int i = 0; i < a.length; i = i + 3) {
						try {
						   Field f = type.getField((String)a[i]);
						   if (f.get(null).equals(oldValue)) {
							   out.remove(oldValue);
							   out.writeExpression(new Expression(store, oldValue, f, "get", new Object[]{null}));
						   }
						}
						catch (Exception ex) {}
					}
				}
				invokeStatement(oldInstance, setter.getName(), new Object[]{oldValue}, out);
			}
		}
	}

	void invokeStatement(Object instance, String methodName, Object[] args, Encoder out) {
		out.writeStatement(new Statement(store, instance, methodName, args));
	}

	// Write out the properties of this instance.
	private void initBean(Class type, Object oldInstance, Object newInstance, Encoder out) {
		// System.out.println("initBean: " + oldInstance);
		BeanInfo info = MetaData.getBeanInfo(type);

		// Properties
		PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
		for (int i = 0; i < propertyDescriptors.length; ++i ) {
			try {
				doProperty(type, propertyDescriptors[i], oldInstance, newInstance, out);
			} catch (Exception e) {
				out.getExceptionListener().exceptionThrown(e);
			}
		}

		// Listeners
		/*
		Pending(milne). There is a general problem with the archival of
		listeners which is unresolved as of 1.4. Many of the methods
		which install one object inside another (typically "add" methods
		or setters) automatically install a listener on the "child" object
		so that its "parent" may respond to changes that are made to it.
		For example the JTable:setModel() method automatically adds a
		TableModelListener (the JTable itself in this case) to the supplied
		table model.

		We do not need to explictly add these listeners to the model in an
		archive as they will be added automatically by, in the above case,
		the JTable's "setModel" method. In some cases, we must specifically
		avoid trying to do this since the listener may be an inner class
		that cannot be instantiated using public API.

		No general mechanism currently
		exists for differentiating between these kind of listeners and
		those which were added explicitly by the user. A mechanism must
		be created to provide a general means to differentiate these
		special cases so as to provide reliable persistence of listeners
		for the general case.
		*/
		if (!java.awt.Component.class.isAssignableFrom(type)) {
			return; // Just handle the listeners of Components for now.
		}
		EventSetDescriptor[] eventSetDescriptors = info.getEventSetDescriptors();
		for (int e = 0; e < eventSetDescriptors.length; e++) {
			EventSetDescriptor d = eventSetDescriptors[e];
			Class listenerType = d.getListenerType();

			// The ComponentListener is added automatically, when
			// Contatiner:add is called on the parent.
			if (listenerType == java.awt.event.ComponentListener.class) {
				continue;
			}

			// JMenuItems have a change listener added to them in
			// their "add" methods to enable accessibility support -
			// see the add method in JMenuItem for details. We cannot
			// instantiate this instance as it is a private inner class
			// and do not need to do this anyway since it will be created
			// and installed by the "add" method. Special case this for now,
			// ignoring all change listeners on JMenuItems.
			if (listenerType == javax.swing.event.ChangeListener.class &&
				type == javax.swing.JMenuItem.class) {
				continue;
			}

			EventListener[] oldL = new EventListener[0];
			EventListener[] newL = new EventListener[0];
			try {
				Method m = d.getGetListenerMethod();
				oldL = (EventListener[])MethodUtil.invoke(m, oldInstance, new Object[]{});
				newL = (EventListener[])MethodUtil.invoke(m, newInstance, new Object[]{});
			} catch (Throwable e2) {
				try {
					Method m = type.getMethod("getListeners", new Class[]{Class.class});
					oldL = (EventListener[])MethodUtil.invoke(m, oldInstance, new Object[]{listenerType});
					newL = (EventListener[])MethodUtil.invoke(m, newInstance, new Object[]{listenerType});
				} catch (Exception e3) {
					return;
				}
			}

			// Asssume the listeners are in the same order and that there are no gaps.
			// Eventually, this may need to do true differencing.
			String addListenerMethodName = d.getAddListenerMethod().getName();
			for (int i = newL.length; i < oldL.length; i++) {
				// System.out.println("Adding listener: " + addListenerMethodName + oldL[i]);
				invokeStatement(oldInstance, addListenerMethodName, new Object[]{oldL[i]}, out);
			}

			String removeListenerMethodName = d.getRemoveListenerMethod().getName();
			for (int i = oldL.length; i < newL.length; i++) {
				invokeStatement(oldInstance, removeListenerMethodName, new Object[]{oldL[i]}, out);
			}
		}
	}

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		// System.out.println("DefaultPD:initialize" + type);
		super.initialize(type, oldInstance, newInstance, out);
		if (oldInstance.getClass() == type) { // !type.isInterface()) {
			initBean(type, oldInstance, newInstance, out);
		}
	}
}
