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
package ca.queensu.cs.sail.mailboxmina2.tools;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import ca.queensu.cs.sail.mailboxmina2.storage.ConnectorPSQL;

import com.sampullara.cli.Args;

public class CreateDatabase {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Read command line arguments as specified in CLICreateDatabase Class
		CLICreateDatabase cli = new CLICreateDatabase();
        try {
        	Args.parse(cli, args);
        } catch (Exception e) {
        	System.err.println("Error while parsing command line arguments: " + e.getMessage());
        	Args.usage(cli);
        	System.exit(1);
        }
        
        // Open a connection
        Properties props = new Properties();
        props.setProperty("db_url", cli.connection+"mm2-template-database");
        props.setProperty("username", cli.username);
        props.setProperty("password", cli.password);
        
        ConnectorPSQL connector = new ConnectorPSQL(props);
        Connection connection = connector.getConnection();
        
        // Drop existing Database
        if (cli.drop) {
	        try {
	        	System.out.println("Dropping existing database " + cli.dbname);
	        	Statement statement = connection.createStatement();
	        	statement.execute("DROP DATABASE \"" + cli.dbname + "\"");
	        	System.out.println("Database " + cli.dbname + " successfully dropped!");
	        } catch (SQLException e) {
	        	System.err.println("Error dropping database " + cli.dbname + " : " + e.getMessage());
	        }
        }
        
        // Create the new Database
        try {
        	System.out.println("Creating database " + cli.dbname);
        	Statement statement = connection.createStatement();
        	statement.execute("CREATE DATABASE \"" + cli.dbname + "\" TEMPLATE \"mm2-template-database\"");
        	System.out.println("Database " + cli.dbname + " successfully created!");
        } catch (SQLException e) {
        	System.err.println("Error creating database " + cli.dbname + " : " + e.getMessage());
        }
        
        // Close the connection
        try {
        	connection.close();
        	System.out.println("Connection closed!");
        } catch(SQLException e) {
        	System.err.println("Error closing connection: " + e.getMessage());
        }
        
        

	}
	
}
