/*
 * @(#)Encoder.java	1.18 05/04/29
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import java.beans.ExceptionListener;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import persistence.Connection;

public class Encoder {
	MetaData metaData;
	Connection connection;
	private Map bindings = new IdentityHashMap();
	private ExceptionListener exceptionListener;
	boolean executeStatements = true;
	private Map attributes;

	Encoder(Connection connection) {
		this.connection=connection;
		metaData=new MetaData(connection);
	}

	protected void writeObject(Object o) {
			if (o == this) {
				return;
		}
		PersistenceDelegate info = getPersistenceDelegate(o == null ? null : o.getClass());
		info.writeObject(o, this);
	}

	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	public ExceptionListener getExceptionListener() {
		return (exceptionListener != null) ? exceptionListener : Statement.defaultExceptionListener;
	}
	
	Object getValue(Expression exp) {
		try {
			return (exp == null) ? null : exp.getValue();
		}
		catch (Exception e) {
			getExceptionListener().exceptionThrown(e);
			throw new RuntimeException("failed to evaluate: " + exp.toString());
		}
	}

	public PersistenceDelegate getPersistenceDelegate(Class type) {
		return metaData.getPersistenceDelegate(type);
	}

	public void setPersistenceDelegate(Class type, PersistenceDelegate persistenceDelegate) {
		metaData.setPersistenceDelegate(type, persistenceDelegate);
	}

	public Object remove(Object oldInstance) {
		Expression exp = (Expression)bindings.remove(oldInstance);
		return getValue(exp);
	}

	public Object get(Object oldInstance) {
		if (oldInstance == null || oldInstance == this ||
			oldInstance.getClass() == String.class) {
			return oldInstance;
		}
		Expression exp = (Expression)bindings.get(oldInstance);
		return getValue(exp);
	}
	
	private Object writeObject1(Object oldInstance) {
		Object o = get(oldInstance);
		if (o == null) { 
			writeObject(oldInstance);  
			o = get(oldInstance);
		}		
		return o;
	}
	
	private Statement cloneStatement(Statement oldExp) {
		Object oldTarget = oldExp.getTarget();
		Object newTarget = writeObject1(oldTarget);
				
		Object[] oldArgs = oldExp.getArguments();
		Object[] newArgs = new Object[oldArgs.length];
		for (int i = 0;i < oldArgs.length;i++) {
			newArgs[i] = writeObject1(oldArgs[i]);
		}
		if (oldExp.getClass() == Statement.class) {
			return new Statement(connection, newTarget, oldExp.getMethodName(), newArgs);
		}
		else {
			return new Expression(connection, newTarget, oldExp.getMethodName(), newArgs);
		}
	}

	public void writeStatement(Statement oldStm) {
		// System.out.println("writeStatement: " + oldExp);
		Statement newStm = cloneStatement(oldStm);
		if (oldStm.getTarget() != this && executeStatements) {
			try { 
				newStm.execute(); 
			}catch (Exception e) { 
				getExceptionListener().exceptionThrown ( new Exception ("Encoder: discarding statement " + newStm, e));
			}

		}
	}

	public void writeExpression(Expression oldExp) {
		// System.out.println("Encoder::writeExpression: " + oldExp);
		Object oldValue = getValue(oldExp);
		if (get(oldValue) != null) { 
			return;
		}
		bindings.put(oldValue, (Expression)cloneStatement(oldExp));
		writeObject(oldValue);
	}
	
	void clear() {
		bindings.clear();
	}

	// Package private method for setting an attributes table for the encoder
	void setAttribute(Object key, Object value) {
		if (attributes == null) {
			attributes = new HashMap();
		}
		attributes.put(key, value);
	}

	Object getAttribute(Object key) {
		if (attributes == null) {
			return null;
		}
		return attributes.get(key);
	}
}
