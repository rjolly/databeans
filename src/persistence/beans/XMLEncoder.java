/*
 * @(#)XMLEncoder.java	1.27 05/04/29
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import java.beans.Introspector;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Vector;
import persistence.Store;
import persistence.PersistentArray;

public class XMLEncoder extends Encoder {

	private static String encoding = "UTF-8";

	private OutputStream out;
	private Object owner;
	private int indentation = 0;
	private boolean internal = false;
	private Map valueToExpression;
	private Map targetToStatementList;
	private boolean preambleWritten = false;
	private NameGenerator nameGenerator;

	private class ValueData {
		public int refs = 0;
		public boolean marked = false;// Marked -> refs > 0 unless ref was a target.
		public String name = null;
		public Expression exp = null;
	}

	public XMLEncoder(Store store, OutputStream out) {
		super(store);
		this.out = out;
		valueToExpression = new IdentityHashMap();
		targetToStatementList = new IdentityHashMap();
		nameGenerator = new NameGenerator();
	}

	public void setOwner(Object owner) {
		this.owner = owner;
		writeExpression(new Expression(store, this, "getOwner", new Object[0]));
	}

	public Object getOwner() {
		return owner;
	}

	public void writeObject(Object o) {
		if (internal) {
			super.writeObject(o);
		}
		else {
			writeStatement(new Statement(store, this, "writeObject", new Object[]{o}));
		}
	}

	private Vector statementList(Object target) {
		Vector list = (Vector)targetToStatementList.get(target);
		if (list != null) {
			return list;
		}
		list = new Vector();
		targetToStatementList.put(target, list);
		return list;
	}

	
	private void mark(Object o, boolean isArgument) {
		if (o == null || o == this) {
			return;
		}
		ValueData d = getValueData(o);
		Expression exp = d.exp;
		// Do not mark liternal strings. Other strings, which might,  
		// for example, come from resource bundles should still be marked. 
		if (o.getClass() == String.class && exp == null) { 
			return;
		} 
		
		// Bump the reference counts of all arguments
		if (isArgument) {
			d.refs++;
		}
		if (d.marked) {
			return;
		}
		d.marked = true;
		Object target = exp.getTarget();
		if (!(target instanceof Class)) {
			statementList(target).add(exp);
			// Pending: Why does the reference count need to
			// be incremented here?
			d.refs++;
		}
		mark(exp);
	}
	
	private void mark(Statement stm) {
		Object[] args = stm.getArguments();
		for (int i = 0;i < args.length;i++) {
			Object arg = args[i];
			mark(arg, true);
		}
		mark(stm.getTarget(), false);
	}

	public void writeStatement(Statement oldStm) {
			// System.out.println("XMLEncoder::writeStatement: " + oldStm);
		boolean internal = this.internal;
		this.internal = true;
		try {
			super.writeStatement(oldStm);
			/*
			   Note we must do the mark first as we may
			   require the results of previous values in
			   this context for this statement.
			   Test case is:
				   os.setOwner(this);
				   os.writeObject(this);
			*/
			mark(oldStm);
			statementList(oldStm.getTarget()).add(oldStm);
		}
		catch (Exception e) {
			getExceptionListener().exceptionThrown(new Exception("XMLEncoder: discarding statement " + oldStm, e));
		}
		this.internal = internal;
	}

	public void writeExpression(Expression oldExp) {
		boolean internal = this.internal;
		this.internal = true;
		Object oldValue = getValue(oldExp);
		if (get(oldValue) == null || (oldValue instanceof String && !internal)) {
			getValueData(oldValue).exp = oldExp;
			super.writeExpression(oldExp);
		}
		this.internal = internal;
	}

	public void flush() {
		if (!preambleWritten) { // Don't do this in constructor - it throws ... pending.
			writeln("<?xml version=" + quote("1.0") +
						" encoding=" + quote(encoding) + "?>");
			writeln("<java version=" + quote(System.getProperty("java.version")) +
						   " class=" + quote(XMLDecoder.class.getName()) + ">");
			preambleWritten = true;
		}
		indentation++;
		Vector roots = statementList(this);
		for(int i = 0;i < roots.size();i++) {
			Statement s = (Statement)roots.get(i);
			if (s.getMethodName() == "writeObject") {
				outputValue(s.getArguments()[0], this, true);
			}
			else {
				outputStatement(s, this, false);
			}
		}
		indentation--;

		try {
			out.flush();
		}
		catch (IOException e) {
			getExceptionListener().exceptionThrown(e);
		}
		clear();
	}

	void clear() { 
		super.clear();
		nameGenerator.clear();
		valueToExpression.clear();
		targetToStatementList.clear();
	}

	public void close() {
		flush();
		writeln("</java>");
		try {
			out.close();
		}
		catch (IOException e) {
			getExceptionListener().exceptionThrown(e);
		}
	}

	private String quote(String s) {
		return "\"" + s + "\"";
	}

	private ValueData getValueData(Object o) {
		ValueData d = (ValueData)valueToExpression.get(o);
		if (d == null) {
			d = new ValueData();
			valueToExpression.put(o, d);
		}
		return d;
	}

	private static String quoteCharacters(String s) {
		StringBuffer result = null;
		for(int i = 0, max = s.length(), delta = 0;i < max;i++) {
			char c = s.charAt(i);
			String replacement = null;

			if (c == '&') {
				replacement = "&amp;";
			} else if (c == '<') {
				replacement = "&lt;";
			} else if (c == '\r') {
				replacement = "&#13;";
			} else if (c == '>') {
				replacement = "&gt;";
			} else if (c == '"') {
				replacement = "&quot;";
			} else if (c == '\'') {
				replacement = "&apos;";
			}
			
			if (replacement != null) {
				if (result == null) {
					result = new StringBuffer(s);
				}
				result.replace(i + delta, i + delta + 1, replacement);
				delta += (replacement.length() - 1);
			}
		}
		if (result == null) {
			return s;
		}
		return result.toString();
	}

	private void writeln(String exp) {
		try {
			for(int i = 0;i < indentation;i++) {
				out.write(' ');
			}
			out.write(exp.getBytes(encoding));
			out.write(" \n".getBytes());
		}
		catch (IOException e) {
			getExceptionListener().exceptionThrown(e);
		}
	}

	private void outputValue(Object value, Object outer, boolean isArgument) {
		if (value == null) {
			writeln("<null/>");
			return;
		}

		if (value instanceof Class) {
			writeln("<class>" + ((Class)value).getName() + "</class>");
			return;
		}

		ValueData d = getValueData(value);		 
		if (d.exp != null && d.exp.getTarget() instanceof Field && d.exp.getMethodName() == "get") {
			Field f = (Field)d.exp.getTarget();
			writeln("<object class=" + quote(f.getDeclaringClass().getName()) + " field=" + quote(f.getName()) + "/>");
			return;
		}		
		
		Class primitiveType = ReflectionUtils.primitiveTypeFor(value.getClass());
		if (primitiveType != null && d.exp.getTarget() == value.getClass() && d.exp.getMethodName() == "new") {
			String primitiveTypeName = primitiveType.getName();
			// Make sure that character types are quoted correctly.
			if (primitiveType == Character.TYPE) {
				value = quoteCharacters(((Character)value).toString());
			}
			writeln("<" + primitiveTypeName + ">" + value + "</" + primitiveTypeName + ">");
			return;
		}

		if (value instanceof String && d.exp == null) {
			writeln("<string>" + quoteCharacters((String)value) + "</string>");
			return;
		}

		if (d.name != null) {
			writeln("<object idref=" + quote(d.name) + "/>");
			return;
		}

		outputStatement(d.exp, outer, isArgument);
	}

	private void outputStatement(Statement exp, Object outer, boolean isArgument) {
		Object target = exp.getTarget();
		String methodName = exp.getMethodName();
		Object[] args = exp.getArguments();
		boolean expression = exp.getClass() == Expression.class;
		Object value = (expression) ? getValue((Expression)exp) : null;

		String tag = (expression && isArgument) ? "object" : "void";
		String attributes = "";
		ValueData d = getValueData(value);
		if (expression) {
			if (d.refs > 1) {
				String instanceName = nameGenerator.instanceName(value);
				d.name = instanceName;
				attributes = attributes + " id=" + quote(instanceName);
			}
		}

		// Special cases for targets.
		if (target == outer) {
		}
		else if (target == Array.class && methodName == "newInstance") {
			tag = "array";
			attributes = attributes + " class=" + quote(((Class)args[0]).getName());
			attributes = attributes + " length=" + quote(args[1].toString());
			args = new Object[]{};
		}
		else if (target == PersistentArray.class && methodName == "newInstance") {
			tag = "persistentArray";
			attributes = attributes + " typeCode=" + quote(args[0].toString());
			attributes = attributes + " length=" + quote(args[1].toString());
			args = new Object[]{};
		}
		else if (target.getClass() == Class.class) {
			attributes = attributes + " class=" + quote(((Class)target).getName());
		}
		else {
			d.refs = 2;
			outputValue(target, outer, false);
			outputValue(value, outer, false);
			return;
		}


		// Special cases for methods.
		if ((!expression && methodName == "set" && args.length == 2 && args[0] instanceof Integer) ||
			 (expression && methodName == "get" && args.length == 1 && args[0] instanceof Integer)) {
			attributes = attributes + " index=" + quote(args[0].toString());
			args = (args.length == 1) ? new Object[]{} : new Object[]{args[1]};

		}
		else if ((!expression && methodName.startsWith("set") && args.length == 1) ||
				  (expression && methodName.startsWith("get") && args.length == 0)) {
			attributes = attributes + " property=" +  
				quote(Introspector.decapitalize(methodName.substring(3)));
		}
		else if (methodName != "new" && methodName != "newInstance") {
			attributes = attributes + " method=" + quote(methodName);
		}

		Vector statements = statementList(value);
		// Use XML's short form when there is no body.
		if (args.length == 0 && statements.size() == 0) {
			writeln("<" + tag + attributes + "/>");
			return;
		}

		writeln("<" + tag + attributes + ">");
		indentation++;

		for(int i = 0;i < args.length;i++) {
			outputValue(args[i], null, true);
		}

		for(int i = 0;i < statements.size();i++) {
			Statement s = (Statement)statements.get(i);
			outputStatement(s, value, false);
		}

		indentation--;
		writeln("</" + tag + ">");
	}
}
