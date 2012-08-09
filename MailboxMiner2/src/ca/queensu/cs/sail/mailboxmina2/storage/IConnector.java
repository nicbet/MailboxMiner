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
package ca.queensu.cs.sail.mailboxmina2.storage;

import java.sql.Connection;
import java.util.Properties;

import javax.mail.Message;


/**
 * An interface describing the Connector classes.
 * As we only support PostgreSQL currently, there is
 * no use of this interface. It's mere existence is
 * only justified by the possibility to extend later.
 * @author Nicolas Bettenburg
 * @deprecated
 *
 */
public interface IConnector {

	public boolean storeMessage(Message message);
	public int reconstructThreads();
	public int cleanBodyText();
	
	public Connection openConnection(Properties props);
}
