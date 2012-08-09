/*******************************************************************************
 * Copyright (c) 2012 Nicolas Bettenburg.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Nicolas Bettenburg - initial API and implementation
 ******************************************************************************/
package ca.queensu.cs.sail.mailboxmina2.main.modules;

import java.util.Properties;

import ca.queensu.cs.sail.mailboxmina2.common.Logger;

/**
 * The IModule interface is used to describe single modules that can be invoked
 * and provides the flexibility for later extension.
 * @author Nicolas Bettenburg
 *
 */
public interface IModule {
	
	/**
	 * Run the Modules main routine
	 * @param props Properties that should be handed to the module
	 * @param logger a Logger() instance to be used with the module
	 * @return true if the module could complete the task successfully, false otherwise
	 */
	public boolean run(Properties props, Logger logger);
	
	/**
	 * Get a textual description of the module
	 * @return a String representing the textual description of the module.
	 */
	public String getDescription();
	
}
