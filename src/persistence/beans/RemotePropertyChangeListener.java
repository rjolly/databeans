/*
 * @(#)PropertyChangeListener.java	1.14 00/02/02
 *
 * Copyright 1996-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package persistence.beans;

import java.beans.PropertyChangeEvent;
import java.rmi.RemoteException;
import persistence.Persistent;

public interface RemotePropertyChangeListener extends java.util.EventListener, Persistent {

	void propertyChange(PropertyChangeEvent evt) throws RemoteException;

}
