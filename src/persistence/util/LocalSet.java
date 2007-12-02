/*
 * @(#)Collections.java	1.66 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package persistence.util;

import java.util.Set;
import persistence.util.RemoteSet;

class LocalSet extends LocalCollection implements Set {
	RemoteSet s;

	LocalSet(RemoteSet s) {
		super(s);
		this.s=s;
	}
}
