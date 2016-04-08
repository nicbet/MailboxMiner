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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ca.queensu.cs.sail.mailboxmina2.common.Logger;
import ca.queensu.cs.sail.mailboxmina2.common.Personality;
import ca.queensu.cs.sail.mailboxmina2.storage.ConnectorPSQL;
import ca.queensu.cs.sail.mailboxmina2.storage.MM2Message;

public class PersonalitiesModule implements IModule {
	
	
	
	@Override
	public String getDescription() {
		return "The Personalities module\n" +
		"----------------------------------------------------------------------------\n" +
		"Input: 	Database connection credentials" +
		"Output: 	Merges Multiple Personalitites Information from MM2 database content.";
	}

	@Override
	public boolean run(Properties props, Logger logger) {
		boolean success = true;
		
		// Open a connection
		ConnectorPSQL connector = new ConnectorPSQL(props);
		Connection connection = connector.getConnection();
		if (connection == null) {
			logger.error(this, "Cannot open connection - aborting personalities module!");
			return false;
		}
		
		// Remove all existing threads information
		logger.debug(1, "Running personalities merging...");		
				
		// Run each of the heuristics
		logger.debug(1, "Running heuristic...");
		
		List<Personality> personalitiesList = new ArrayList<Personality>();
		
		try {
			Statement stmt = connection.createStatement();
			stmt.executeQuery("SELECT count(msg_id) as amount, msg_sender_name, msg_sender_address " +
					"FROM messages " +
					"GROUP BY msg_sender_address, msg_sender_name " +
					"ORDER BY msg_sender_address");
			
			ResultSet results = stmt.getResultSet();
			while (results.next()) {
				String name = results.getString("msg_sender_name");
				String addr = results.getString("msg_sender_address");
				int count = results.getInt("amount");
				Personality p = new Personality(name, addr, count);
				personalitiesList.add(p);
			}
			
			stmt.close();
			System.out.println(personalitiesList.size());
		} catch (SQLException e) {
			logger.error(this, "Error", e);
		}
		
				
		// Done
		return success;
	}
	

	
}
