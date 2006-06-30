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

import java.beans.*;
import java.rmi.*;

public interface RemotePropertyChangeListener extends java.util.EventListener, Remote {

	void propertyChange(PropertyChangeEvent evt) throws RemoteException;

}
