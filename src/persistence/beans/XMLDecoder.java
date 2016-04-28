/*
 * @(#)XMLDecoder.java	1.22 05/04/29
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import java.beans.ExceptionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;
import persistence.Store;

public class XMLDecoder {
	Store store;
	private InputStream in;
	private Object owner;
	private ExceptionListener exceptionListener;
	private ObjectHandler handler;

	public XMLDecoder(Store store, InputStream in) {
		this(store, in, null);
	}

	public XMLDecoder(Store store, InputStream in, Object owner) {
		this(store, in, owner, null);
	}

	public XMLDecoder(Store store, InputStream in, Object owner, ExceptionListener exceptionListener) {
		this.in = in;
		this.store = store;
		setOwner(owner);
		setExceptionListener(exceptionListener);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
		   SAXParser saxParser = factory.newSAXParser();
		   handler = new ObjectHandler(this);
		   saxParser.parse(in, handler);
		} catch (ParserConfigurationException e) {
			getExceptionListener().exceptionThrown(e);
		} catch (SAXException se) {
			Exception e = se.getException();
			getExceptionListener().exceptionThrown((e == null) ? se : e);
		} catch (IOException ioe) {
			getExceptionListener().exceptionThrown(ioe);
		}
	}

	public void close() {
		if (in != null) {
			try {
				in.close();
			}catch (IOException e) {
				getExceptionListener().exceptionThrown(e);
			}
		}
	}

	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	public ExceptionListener getExceptionListener() {
		return (exceptionListener != null) ? exceptionListener :
			Statement.defaultExceptionListener;
	}

	public Object readObject() {
		return handler.dequeueResult();
	}

	public void setOwner(Object owner) {
		this.owner = owner;
	}

	public Object getOwner() {
			return owner;
	}
}

class MutableExpression extends Expression {
	private Object property;
	private Vector argV = new Vector();

	private String capitalize(String propertyName) {
		if (propertyName.length() == 0) {
			return propertyName;
		}
		return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
	}

	public MutableExpression(Store store) {
		super(store, null, null, null);
	}

	public Object[] getArguments() {
			return argV.toArray();
	}

	public String getMethodName() {
		if (property == null) {
			return super.getMethodName();
		}
		int setterArgs = (property instanceof String) ? 1 : 2;
		String methodName = (argV.size() == setterArgs) ? "set" : "get";
		if (property instanceof String) {
			return methodName + capitalize((String)property);
		} else {
			return methodName;
		}
	}

	public void addArg(Object arg) {
		argV.add(arg);
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public void setProperty(Object property) {
		this.property = property;
	}
}

class ObjectHandler extends HandlerBase {

	public static Class typeNameToClass(String typeName) {
	  typeName = typeName.intern();
	  if (typeName == "boolean") return Boolean.class;
	  if (typeName == "byte") return Byte.class;
	  if (typeName == "char") return Character.class;
	  if (typeName == "short") return Short.class;
	  if (typeName == "int") return Integer.class;
	  if (typeName == "long") return Long.class;
	  if (typeName == "float") return Float.class;
	  if (typeName == "double") return Double.class;
	  if (typeName == "void") return Void.class;
	  return null;
	}

	public static Class typeNameToPrimitiveClass(String typeName) {
	  typeName = typeName.intern();
	  if (typeName == "boolean") return boolean.class;
	  if (typeName == "byte") return byte.class;
	  if (typeName == "char") return char.class;
	  if (typeName == "short") return short.class;
	  if (typeName == "int") return int.class;
	  if (typeName == "long") return long.class;
	  if (typeName == "float") return float.class;
	  if (typeName == "double") return double.class;
	  if (typeName == "void") return void.class;
	  return null;
	}

	private Hashtable environment;
	private Vector expStack;
	private StringBuffer chars;
	private XMLDecoder is;
	private int itemsRead = 0;

	public ObjectHandler(XMLDecoder is) {
		environment = new Hashtable();
		expStack = new Vector();
		chars = new StringBuffer();
		this.is = is;
	}

	private Object getValue(Expression exp) {
		try {
			return exp.getValue();
		} catch (Exception e) {
			is.getExceptionListener().exceptionThrown(e);
			return null;
		}
	}

	private void addArg(Object arg) {
		// System.out.println("addArg: " + instanceName(arg));
		lastExp().addArg(arg);
	}

	private Object pop(Vector v) {
		int last = v.size() - 1;
		Object result = v.get(last);
		v.remove(last);
		return result;
	}

	private Object eval() {
		return getValue((Expression)lastExp());
	}

	private MutableExpression lastExp() {
		return (MutableExpression)expStack.lastElement();
	}

	Object dequeueResult() {
		// System.out.println("dequeueResult: " + expStack);
		Object[] results = lastExp().getArguments();
		return results[itemsRead++];
	}

	private boolean isPrimitive(String name) {
		return name != "void" && typeNameToClass(name) != null;
	}

	private void simulateException(String message) {
		Exception e = new Exception(message);
		e.fillInStackTrace();
		is.getExceptionListener().exceptionThrown(e);
	}

	static Class classForName(String name) throws ClassNotFoundException {
	  Class primitiveType = typeNameToPrimitiveClass(name);
	  if (primitiveType != null) {
		  return primitiveType;
	  }
	  return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
	}

	private Class classForName2(String name) {
		try {
			return classForName(name);
		} catch (ClassNotFoundException e) {
			is.getExceptionListener().exceptionThrown(e);
		}
		return null;
	}

	private HashMap getAttributes(AttributeList attrs) {
		HashMap attributes = new HashMap();
		if (attrs != null && attrs.getLength() > 0) {
			for(int i = 0;i < attrs.getLength();i++) {
				attributes.put(attrs.getName(i), attrs.getValue(i));
			}
		}
		return attributes;
	}

	public void startElement(String name, AttributeList attrs) throws SAXException {
		// System.out.println("startElement" + name);
		name = name.intern();// Xerces parser does not supply unique tag names.
		chars.setLength(0);
		if (name == "null" ||
			name == "string" ||
			name == "class" ||
			isPrimitive(name)) {
			return;
		}
		HashMap attributes = getAttributes(attrs);

		MutableExpression e = new MutableExpression(is.store);

		// Target
		String className = (String)attributes.get("class");
		if (className != null) {
			e.setTarget(classForName2(className));
		}

		// Property
		Object property = attributes.get("property");
		String index = (String)attributes.get("index");
		if (index != null) {
			property = new Integer(index);
			e.addArg(property);
		}
		e.setProperty(property);

		// Method
		String methodName = (String)attributes.get("method");
		if (methodName == null && property == null) {
			methodName = "new";
		}
		e.setMethodName(methodName);

		// Tags
		if (name == "void") {
			if (e.getTarget() == null) {// this check is for "void class="foo" method= ..."
				e.setTarget(eval());
			}
		} else if (name == "array") {
			// The class attribute means sub-type for arrays.
			String subtypeName = (String)attributes.get("class");
			Class subtype = (subtypeName == null) ? Object.class : classForName2(subtypeName);
			String length = (String)attributes.get("length");
			if (length != null) {
				e.setTarget(java.lang.reflect.Array.class);
				e.addArg(subtype);
				e.addArg(new Integer(length));
			} else {
				Class arrayClass = java.lang.reflect.Array.newInstance(subtype, 0).getClass();
				e.setTarget(arrayClass);
			}
		} else if (name == "persistentArray") {
			// The class attribute means sub-type for arrays.
			String subtypeName = (String)attributes.get("typeCode");
			String length = (String)attributes.get("length");
			e.setTarget(persistence.PersistentArray.class);
			e.addArg(new Character((subtypeName!=null?subtypeName:"L").charAt(0)));
			e.addArg(new Integer(length!=null?length:"0"));
		} else if (name == "java") {
			e.setValue(is);// The outermost scope is the stream itself.
		} else if (name == "object") {
		} else {
			simulateException("Unrecognized opening tag: " + name + " " + attrsToString(attrs));
			return;
		}

		// ids
		String idName = (String)attributes.get("id");
		if (idName != null) {
			environment.put(idName, e);
		}

		// idrefs
		String idrefName = (String)attributes.get("idref");
		if (idrefName != null) {
			e.setValue(lookup(idrefName));
		}

		// fields
		String fieldName = (String)attributes.get("field");
		if (fieldName != null) {
			e.setValue(getFieldValue(e.getTarget(), fieldName));
		}
		expStack.add(e);
	}

	private Object getFieldValue(Object target, String fieldName) {
		try {
			Class type = target.getClass();
			if (type == Class.class) {
				type = (Class)target;
			}
			java.lang.reflect.Field f = type.getField(fieldName);
			return f.get(target);
		} catch (Exception e) {
			is.getExceptionListener().exceptionThrown(e);
			return null;
		}
	}

	private String attrsToString(AttributeList attrs) {
		StringBuffer b = new StringBuffer();
		for (int i = 0;i < attrs.getLength ();i++) {
			b.append(attrs.getName(i)+"=\""+attrs.getValue(i)+"\" ");
		}
		return b.toString();
	}

	public void characters(char buf [], int offset, int len) throws SAXException {
		chars.append(new String(buf, offset, len));
	}

	private Object lookup(String s) {
		Expression e = (Expression)environment.get(s);
		if (e == null) {
			simulateException("Unbound variable: " + s);
		}
		return getValue(e);
	}

	public void endElement(String name) throws SAXException {
		// System.out.println("endElement: " + expStack);
		name = name.intern();// Xerces parser does not supply unique tag names.
		if (name == "null") {
			addArg(null);
			return;
		}
		if (name == "java") {
			return;
		}
		if (name == "string") {
			addArg(chars.toString());
			return;
		}
		if (name == "class") {
			addArg(classForName2(chars.toString()));
			return;
		}
		if (isPrimitive(name)) {
			Class wrapper = typeNameToClass(name);
			Expression e = new Expression(is.store, wrapper, "new", new Object[]{chars.toString()});
			addArg(getValue(e));
			return;
		}
		if (name == "object" || name == "array" || name == "persistentArray" || name == "void") {
			Expression e = (Expression)pop(expStack);
			Object value = getValue(e);
			if (name == "object" || name == "persistentArray" || name == "array") {
				addArg(value);
			}
		} else {
			simulateException("Unrecognized closing tag: " + name);
		}
	}
}
