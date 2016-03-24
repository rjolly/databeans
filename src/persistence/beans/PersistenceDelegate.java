/*
 * @(#)PersistenceDelegate.java	1.7 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.beans;

import persistence.Store;

public abstract class PersistenceDelegate {
	Store store;

	PersistenceDelegate(Store store) {
		this.store=store;
	}

	public void writeObject(Object oldInstance, Encoder out) {
		// System.out.println("PersistenceDelegate::writeObject " + NameGenerator.instanceName(oldInstance));
		Object newInstance = out.get(oldInstance);
		if (!mutatesTo(oldInstance, newInstance)) {
			out.remove(oldInstance);
			out.writeExpression(instantiate(oldInstance, out));
		}
		else {
			initialize(oldInstance.getClass(), oldInstance, newInstance, out);
		}
	}

	protected boolean mutatesTo(Object oldInstance, Object newInstance) {
		return (newInstance != null &&
				oldInstance.getClass() == newInstance.getClass());
	}

	protected abstract Expression instantiate(Object oldInstance, Encoder out);

	protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
		// System.out.println("initialize: " + NameGenerator.instanceName(oldInstance));
		Class superType = type.getSuperclass();
		PersistenceDelegate info = out.getPersistenceDelegate(superType);
		info.initialize(superType, oldInstance, newInstance, out);
	}
}
