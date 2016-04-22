/*
 * @(#)Statement.java	1.23 05/06/07
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import java.beans.ExceptionListener;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import persistence.Store;
import persistence.PersistentArray;
import persistence.PersistentObject;

public class Statement {
	Store store;

	private static Object[] emptyArray = new Object[]{};

	static ExceptionListener defaultExceptionListener = new ExceptionListener() {
		public void exceptionThrown(Exception e) {
			System.err.println(e);
			// e.printStackTrace();
			System.err.println("Continuing ...");
		}
	};

	Object target;
	String methodName;
	Object[] arguments;

	public Statement(Store store, Object target, String methodName, Object[] arguments) {
		this.store = store;
		this.target = target;
		this.methodName = methodName;
		this.arguments = (arguments == null) ? emptyArray : arguments;
	}

	public Object getTarget() {
		return target;
	}

	public String getMethodName() {
		return methodName;
	}

	public Object[] getArguments() {
		return arguments;
	}

	public void execute() throws Exception {
		invoke();
	}

	/*static Class classForName(String name) throws ClassNotFoundException {
		// l.loadClass("int") fails.
		Class primitiveType = typeNameToPrimitiveClass(name);
		if (primitiveType != null) {
			return primitiveType;
		}
		ClassLoader l = Thread.currentThread().getContextClassLoader();
		return l.loadClass(name);
	}*/

	Object invoke() throws Exception {
		Object target = getTarget();
		String methodName = getMethodName();

		if (target == null || methodName == null) {
			throw new NullPointerException((target == null ? "target" : "methodName") + " should not be null");
		}

		Object[] arguments = getArguments();
		// Class.forName() won't load classes outside
		// of core from a class inside core. Special
		// case this method.
		if (target == Class.class && methodName == "forName") {
			return ObjectHandler.classForName((String)arguments[0]);
		}
		Class[] argClasses = new Class[arguments.length];
		for(int i = 0;i < arguments.length;i++) {
			argClasses[i] = (arguments[i] == null) ? null : arguments[i].getClass();
		}

		AccessibleObject m = null;
		if (target instanceof Class) {
			/*
			For class methods, simluate the effect of a meta class
			by taking the union of the static methods of the
			actual class, with the instance methods of "Class.class"
			and the overloaded "newInstance" methods defined by the
			constructors.
			This way "System.class", for example, will perform both
			the static method getProperties() and the instance method
			getSuperclass() defined in "Class.class".
			*/
			if (methodName == "new") {
				methodName = "newInstance";
			}
			// Provide a short form for array instantiation by faking an nary-constructor.
			if (methodName == "newInstance" && ((Class)target).isArray()) {
				Object result = Array.newInstance(((Class)target).getComponentType(), arguments.length);
				for(int i = 0;i < arguments.length;i++) {
					Array.set(result, i, arguments[i]);
				}
				return result;
			}
			if (methodName == "newInstance" && target == PersistentArray.class) {
				return new PersistentArray(store, ReflectionUtils.componentTypeFor(((Character)arguments[0]).charValue()), ((Integer)arguments[1]).intValue());
			}
			if (methodName == "newInstance" && PersistentObject.class.isAssignableFrom((Class)target)) {
				return ((Class)target).getConstructor(Store.class).newInstance(store);
			}
			if (methodName == "newInstance" && arguments.length != 0) {
				// The Character class, as of 1.4, does not have a constructor
				// which takes a String. All of the other "wrapper" classes
				// for Java's primitive types have a String constructor so we
				// fake such a constructor here so that this special case can be
				// ignored elsewhere.
				if (target == Character.class && arguments.length == 1 && argClasses[0] == String.class) {
					return new Character(((String)arguments[0]).charAt(0));
				}
				m = ReflectionUtils.getConstructor((Class)target, argClasses);
			}
			if (m == null && target != Class.class) {
				m = ReflectionUtils.getMethod((Class)target, methodName, argClasses);
			}
			if (m == null) {
				m = ReflectionUtils.getMethod(Class.class, methodName, argClasses);
			}
		} else {
			/*
			This special casing of arrays is not necessary, but makes files
			involving arrays much shorter and simplifies the archiving infrastrcure.
			The Array.set() method introduces an unusual idea - that of a static method
			changing the state of an instance. Normally statements with side
			effects on objects are instance methods of the objects themselves
			and we reinstate this rule (perhaps temporarily) by special-casing arrays.
			*/
			if (target.getClass().isArray() && (methodName == "set" || methodName == "get")) {
				int index = ((Integer)arguments[0]).intValue();
				if (methodName == "get") {
					return Array.get(target, index);
				} else {
					Array.set(target, index, arguments[1]);
					return null;
				}
			}
			m = ReflectionUtils.getMethod(target.getClass(), methodName, argClasses);
		}
		if (m != null) {
			try {
				if (m instanceof Method) {
					return MethodUtil.invoke((Method)m, target, arguments);
				} else {
					return ((Constructor)m).newInstance(arguments);
				}
			} catch (IllegalAccessException iae) {
				throw new Exception("Statement cannot invoke: " + methodName + " on " + target.getClass(), iae);
			} catch (InvocationTargetException ite) {
				Throwable te = ite.getTargetException();
				if (te instanceof Exception) {
					throw (Exception)te;
				} else {
					throw ite;
				}
			}
		}
		throw new NoSuchMethodException(toString());
	}

	String instanceName(Object instance) {
		if (instance == null) {
			return "null";
		} else if (instance.getClass() == String.class) {
			return "\""+(String)instance + "\"";
		} else {
			return NameGenerator.unqualifiedClassName(instance.getClass());
		}
	}

	public String toString() {
		// Respect a subclass's implementation here.
		Object target = getTarget();
		String methodName = getMethodName();
		Object[] arguments = getArguments();

		StringBuffer result = new StringBuffer(instanceName(target) + "." + methodName + "(");
		int n = arguments.length;
		for(int i = 0;i < n;i++) {
			result.append(instanceName(arguments[i]));
			if (i != n -1) {
				result.append(", ");
			}
		}
		result.append(");");
		return result.toString();
	}
}
