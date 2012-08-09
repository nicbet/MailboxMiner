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
package ca.queensu.cs.sail.mailboxmina2.test;

import ca.queensu.cs.sail.mailboxmina2.common.MailboxParser;



public class MBoxReadTest2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String storageFolder = args[0];
		MailboxParser p = new MailboxParser();
		p.parseMessages(storageFolder);
		
	}
}
