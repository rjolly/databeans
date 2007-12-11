/*
 * @(#)Expression.java	1.10 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import persistence.Connection;

public class Expression extends Statement {

	private static Object unbound = new Object();
	
	private Object value = unbound;

	public Expression(Connection connection, Object target, String methodName, Object[] arguments) {
		super(connection, target, methodName, arguments);
	} 

	public Expression(Connection connection, Object value, Object target, String methodName, Object[] arguments) {
		this(connection, target, methodName, arguments);
		setValue(value);
	} 

	public Object getValue() throws Exception {
		if (value == unbound) {
			setValue(invoke());
		}
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	} 
	
	/*pp*/ String instanceName(Object instance) {
		return instance == unbound ? "<unbound>" : super.instanceName(instance);
	} 

	public String toString() {
		return instanceName(value) + "=" + super.toString();
	} 
}
