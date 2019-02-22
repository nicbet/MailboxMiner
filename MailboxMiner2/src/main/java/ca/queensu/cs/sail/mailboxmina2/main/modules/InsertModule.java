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

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;

import ca.queensu.cs.sail.mailboxmina2.common.Logger;
import ca.queensu.cs.sail.mailboxmina2.common.MailboxParser;
import ca.queensu.cs.sail.mailboxmina2.storage.ConnectorPSQL;


/**
 * The InsertModule inserts the contents of a directory of .mbox files
 * or the contents of a single .mbox file into a database.
 * Note: The database must already exist and be in the predefined format.
 *       You can use the CreateDatabase::main() method to create a database.
 * @author Nicolas Bettenburg
 *
 */
public class InsertModule implements IModule {
	private Logger logger;
	private ConnectorPSQL storageConnector;
	private boolean testonly;
	
	public InsertModule(boolean testonly) {
		this.testonly = testonly;
	}
	
	/* (non-Javadoc)
	 * @see ca.queensu.cs.sail.mailboxmina2.main.modules.IModule#getDescription()
	 */
	@Override
	public String getDescription() {
		return "The Insert module\n" +
				"----------------------------------------------------------------------------\n" +
				"Input: 	Database connection credentials, path to folder or mbox file\n" +
				"Output: 	Stores the messages contained in the mbox file(s) in the database.";
	}

	/* (non-Javadoc)
	 * @see ca.queensu.cs.sail.mailboxmina2.main.modules.IModule#run(java.util.Properties, ca.queensu.cs.sail.mailboxmina2.common.Logger)
	 */
	@Override
	public boolean run(Properties props, Logger logger) {
		// Declare some local variables
		boolean SUCCESS = true;
				
		// Check for all required properties
		this.checkProperties(props);
		
		// Set the Logger
		this.logger = logger;
		
		// Run the insertion process
		this.storageConnector = new ConnectorPSQL(props);
		try {
			File filepath = new File(props.getProperty("path"));
			if (filepath.isDirectory()) {
				// process all mbox files in the directory
				File[] filesInDirectory = filepath.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						if (name.endsWith(".mbox"))
							return true;
						return false; }
				});
				
				Arrays.sort(filesInDirectory);
				
				for (File f : filesInDirectory) {
					boolean result = storeInDatabase(f);
					SUCCESS = SUCCESS & result;
				}	
			} else if (filepath.isFile()) {
				// process the single file
				SUCCESS = storeInDatabase(filepath);
			} else {
				logger.error(this, "Error! Check the path " + filepath.getAbsolutePath());
				SUCCESS = false;
			}
		} catch (SQLException e) {
			logger.error(this, "SQL Error", e);
			while (e.getNextException() != null) {
				e = e.getNextException();
				logger.error(e.getMessage());
			}
			SUCCESS=false;
		}
		
		return SUCCESS;
	}
	
	/**
	 * Private method to store the contents of a .mbox file in a database
	 * @param filepath The path to the file to be processed
	 */
	private boolean storeInDatabase(File filepath) throws SQLException {
		boolean success = true;
		logger.debug(this, "Processing " + filepath.getAbsolutePath());
		System.out.println("Processing " + filepath.getAbsolutePath());
		
		MailboxParser p = new MailboxParser();
		
		List<Message> messages = p.parseMessages(filepath.getAbsolutePath());
		logger.debug(this, "Extracted " + messages.size() + " messages!");
		logger.debug(this, "Encountered " + p.getCollisions() + " duplicates!");
		
		logger.debug(this, "Inserting messages into Database...");
		Connection connection = storageConnector.getConnection();
		Statement batchStatement = connection.createStatement();

                int nrBadMessages=0;
                int totalNrMessages=0;
                
                for (Message msg : messages) {
                    totalNrMessages++;
                    connection.setAutoCommit(false);
                    if (!this.storageConnector.storeMessage(msg, batchStatement)) {
                        success = false;
                        nrBadMessages++;
                    }else{
                        if (!testonly){
                            try{
                                batchStatement.executeBatch();
                            }catch(SQLException ex){
                                try{
                                    logger.error(this, "Could not insert message: "+ msg.getSubject());
                                }catch(javax.mail.MessagingException e){
                                    logger.error(this, "No subject for message: "+ msg.toString());
                                    nrBadMessages++;
                                }
                            }

                            connection.setAutoCommit(true);
                            try{
                                batchStatement.clearBatch();
                            }catch(SQLException ex){
                                logger.error(this, "Could not clear batch statement!", ex);
                            }
                        }
                    }
                }
                
                batchStatement.close();
                System.out.println(nrBadMessages+"/"+totalNrMessages+" errors encountered");
                return success;
	}

    /**
     * This method checks whether all required properties have been
     * supplied to the Module. If not an error message is printed.
     * @param props
     */
    private void checkProperties(Properties props) {
        if (!props.containsKey("username")
            || !props.containsKey("db_url")
            || !props.containsKey("password")
            || !props.containsKey("path")) {
            logger.error(this, "Error! Please supply all required properties to the module!");
            System.exit(1);
        }
    }

}
