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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateUtils;

import ca.queensu.cs.sail.mailboxmina2.common.Logger;
import ca.queensu.cs.sail.mailboxmina2.main.Main;
import ca.queensu.cs.sail.mailboxmina2.storage.ConnectorPSQL;
import ca.queensu.cs.sail.mailboxmina2.storage.MM2Message;

public class ThreadsModule implements IModule {


	// The offset we allow for the sliding window in the subjects heuristic (in months)
	private static int OFFSET = -6;

	@Override
	public String getDescription() {
		return "The Threads module\n" +
		"----------------------------------------------------------------------------\n" +
		"Input: 	Database connection credentials" +
		"Output: 	Recreates Threads Information from MM2 database content.";
	}

	@Override
	public boolean run(Properties props, Logger logger) {
		boolean success = true;

		// Open a connection
		ConnectorPSQL connector = new ConnectorPSQL(props);
		Connection connection = connector.getConnection();
		if (connection == null) {
			logger.error(this, "Cannot open connection - aborting thread module!");
			return false;
		}
		// Retrieve Messages
		List<MM2Message> messages = connector.getMM2Messages(false, false);

		// Remove all existing threads information
		logger.debug(1, "Running thread generation...");
		try {
			logger.debug(1,"Dropping existing thread associations...");
			dropAssociations(connection);
		} catch (Exception e) {
			logger.warning("Could not delete existing thread data ", e);
		}

		// Run each of the heuristics
		logger.debug(1, "Running heuristic: in-reply...");
		heuristicInreply(messages, connection);

		logger.debug(1, "Running heuristic: references...");
		heuristicReferences(messages, connection);

		logger.debug(1,"Running heuristic: subjects...");
		heuristicSubject(messages, connection);

		// Calculate the roots
		logger.debug(1, "Calculating message roots...");
		try {
			calculateRoots(messages, connection);
		} catch (Exception e) {
			logger.error(this, "Error while calculating message roots!", e);
		}

		// Done
		return success;
	}

	// ------------------------------------------------------------------------------------------------------------
	//                                 Here come the database helper functions
	//								   some time we should decouple the schema and the data!
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Retrieve associations of message ids to messages's parents from the
	 * database
	 *
	 * @return
	 */
	public Hashtable<String, String> getDBThreadsParents(Connection connection) {
		Hashtable<String, String> parentTable = new Hashtable<String, String>();
		String sqlText;
		// Get all the entries in the parent table
		sqlText = "SELECT msg_id, parent_id from parent";
		try {
			Statement statement = connection.createStatement();
			ResultSet res = statement.executeQuery(sqlText);
			while (res.next()) {
				// Store all entries in the hashtable NULL POINTER Exception if one of the entries is null
				parentTable.put(res.getString("msg_id"), res.getString("msg_parent_id"));
			}
		} catch (SQLException e) {
			Main.getLogger().error(this, "Error while receiving message parent ids", e);
		}

		return parentTable;
	}


	/**
	 * Retrieve associations of message ids to messages' roots from the database
	 *
	 * @return
	 */
	public Hashtable<String, String> getDBThreadsRoots(Connection connection) {
		Hashtable<String, String> rootTable = new Hashtable<String, String>();
		String sqlText;
		// Get all the entries in the root table
		sqlText = "SELECT msg_id, root_id from roots";
		try {
			Statement statement = connection.createStatement();
			ResultSet res = statement.executeQuery(sqlText);
			while (res.next()) {
				// Store all entries in the hashtable
				rootTable.put(res.getString("msg_id"), res.getString("root_parent_id"));
			}
		} catch (SQLException e) {
			Main.getLogger().error(this, "Error while receiving message root ids", e);
		}
		return rootTable;
	}


	/**
	 * This method stores associations of parents given as Hash Table
	 * in the Database
	 *
	 * @param parents
	 *            a table that maps for every method having a parent this
	 *            parent's ID
	 * @throws SQLException
	 */
	private void storeParents(Hashtable<String, String> parents, Connection connection) throws SQLException {
		// Use StringBuilders to create big insertion queries

		StringBuilder parentQueryBuilder = new StringBuilder();

		// Queries use the pgpsql functions
		// merge_parent(int msg_uid, int parent_uid)
		// merge_root(int msg_uid, int root_uid)
		// that take care of insertion / update automatically

		parentQueryBuilder.append("PREPARE parentplan (int, int) AS SELECT merge_parent($1, $2);");
		// Genereate the parents query
		int i = 0;
		for (String key : parents.keySet()) {
			parentQueryBuilder.append("EXECUTE parentplan (" + key + ", "
					+ parents.get(key) + ");"
					+ System.getProperty("line.separator"));
			i++;
			if ((i % 1000) == 0) {
				i = 0;
				parentQueryBuilder.append("DEALLOCATE parentplan;" + System.getProperty("line.separator"));
				String sqlstr = parentQueryBuilder.toString();
				Statement statement = connection.createStatement();
				statement.execute(sqlstr);
				statement.close();
				parentQueryBuilder.delete(0,parentQueryBuilder.length());
				parentQueryBuilder.append("PREPARE parentplan (int, int) AS SELECT merge_parent($1, $2);");
				Main.getLogger().debug(4, "Stored 1000 parent relations!");
			}
		}
		if (i > 0) {
			parentQueryBuilder.append("DEALLOCATE parentplan;" + System.getProperty("line.separator"));
			String sqlstr = parentQueryBuilder.toString();
			Statement statement = connection.createStatement();
			statement.execute(sqlstr);
			statement.close();
			Main.getLogger().debug(4, "Stored " + i + " parent relations!");
		}
	}


	/**
	 * This method stores the root associations of a roots table into the database
	 * @param roots a list of roots associations
	 * @throws SQLException
	 */
	private void storeRoots(Hashtable<String, String> roots, Connection connection) throws SQLException {
		// Use StringBuilders to create big insertion queries

		StringBuilder rootQueryBuilder = new StringBuilder();

		// Queries use the pgpsql functions
		// merge_parent(int msg_uid, int parent_uid)
		// merge_root(int msg_uid, int root_uid)
		// that take care of insertion / update automatically

		rootQueryBuilder.append("PREPARE rootplan (int, int) AS SELECT merge_root($1, $2);");
		// Genereate the roots query
		int i = 0;
		for (String key : roots.keySet()) {
			rootQueryBuilder.append("EXECUTE rootplan (" + key + ", "
					+ roots.get(key) + ");"
					+ System.getProperty("line.separator"));
			i++;
			if ((i % 1000) == 0) {
				i = 0;
				rootQueryBuilder.append("DEALLOCATE rootplan;" + System.getProperty("line.separator"));
				String sqlstr = rootQueryBuilder.toString();
				Statement statement = connection.createStatement();
				statement.execute(sqlstr);
				statement.close();
				rootQueryBuilder.delete(0,rootQueryBuilder.length());
				rootQueryBuilder.append("PREPARE rootplan (int, int) AS SELECT merge_root($1, $2);");
				Main.getLogger().debug(4, "Stored 1000 root relations!");
			}
		}
		if (i > 0) {
			rootQueryBuilder.append("DEALLOCATE rootplan;" + System.getProperty("line.separator"));
			String sqlstr = rootQueryBuilder.toString();
			Statement statement = connection.createStatement();
			statement.execute(sqlstr);
			statement.close();
			Main.getLogger().debug(4, "Stored " + i + " root relations!");
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	//                                 Here come the heuristics to re-build threads
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Helper function for the in-reply-to heuristic
	 * Among a list of strings it will return the longst one.
	 * @param elements a {@link List} of {@link String} objects.
	 * @return the longest {@link String} contained in elements.
	 */
	public String getLongestString(List<String> elements) {
		Collections.sort(elements, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (o1.length() > o2.length())
					return 1;
				if (o1.length() < o2.length())
					return -1;
				return 0;
			}
		});
		if (elements.size() == 0)
			return "";
		else
			return elements.get(elements.size()-1);
	}

	/**
	 * Helper function for the in-reply-to heuristic.
	 * Finds the longest of all matches to a given pattern and an input.
	 * @param input a {@link String} to find all matches of {@link Pattern} p in.
	 * @param p a {@link Pattern} to match against
	 * @return the longest match found in input if exists, an empty {@link String} otherwise.
	 */
	public String findLongestPatternMatch(String input, Pattern p) {
		List<String> matches = new ArrayList<String>();
		Matcher matcher = p.matcher(input);
		while (matcher.find())
			matches.add(matcher.group(1));
		return getLongestString(matches);
	}

	/**
	 * This heuristic creates associations based on the in-reply: header field
	 * of messages
	 */
	private void heuristicInreply(List<MM2Message> messages, Connection connection) {
		// This is the msg_id ==> in-reply-to
		Hashtable<String, String> msg_id_to_message_id = new Hashtable<String, String>();

		// This is message-id ==> msg_id
		Hashtable<String, String> message_id_to_msg_id = new Hashtable<String, String>();

		// This is child ==> parent
		Hashtable<String, String> msg_id_to_msg_id = new Hashtable<String, String>();
		Pattern messageid_pattern = Pattern.compile("<(.*?@.*?)>");

		try {
			for (MM2Message msg : messages) {

				// Step One: check whether the message has a message-id header
				// We assume that the "message-id" field is always set - at least to "" and is not null
				String h_message_id = msg.getHeaderEntry("message-id");
				if (h_message_id.length() > 2 ) {
					// We try to identify the message identifier part
					String maybeMatch = findLongestPatternMatch(h_message_id, messageid_pattern);
					if (!maybeMatch.equalsIgnoreCase("")) {
						String extracted_message_id = maybeMatch;
						String msg_id = msg.getHeaderEntry("msg_id");
						// Add the information to the reverse lookup table that we will need later
						message_id_to_msg_id.put(extracted_message_id, msg_id);
						Main.getLogger().debug(5, this, "I know that message " + msg_id + " has message-id " + extracted_message_id);
					}
				}


				// Step Two: check whether the message has an in-reply-to header
				// Again we assume that the "in-reply-to" field is at least "" and not null
				String h_in_reply_to = msg.getHeaderEntry("in-reply-to");
				if(h_in_reply_to.contains(">") && h_in_reply_to.contains("<")) {
					// We try to identify the message identifier part
					String maybeMatch = findLongestPatternMatch(h_in_reply_to, messageid_pattern);
					if (!maybeMatch.equalsIgnoreCase("")) {
						String extracted_in_reply_to_messageid = maybeMatch;
						String msg_id = msg.getHeaderEntry("msg_id");
						// Add the information into the forward lookup table
						Main.getLogger().debug(5, this, "I know that message " + msg_id + " is a reply to " + extracted_in_reply_to_messageid);
						msg_id_to_message_id.put(msg_id, extracted_in_reply_to_messageid);
					}
				}
			}

			// Step Three: After we obtained the previous information, we will
			// resolve each in-reply-to id to a msg_id. so we know for each key in the
			// forward lookup table the msg_id it is a reply to:

			for (String child_msg_id : msg_id_to_message_id.keySet()) {
				String parent_msg_id = message_id_to_msg_id.get(msg_id_to_message_id.get(child_msg_id));
				// If we found an entry in the table
				if (parent_msg_id != null) {
					// Add this information to the parents table
					Main.getLogger().debug(5, this, "I know that message " + child_msg_id + " has the parent " + parent_msg_id);
					msg_id_to_msg_id.put(child_msg_id, parent_msg_id);
				}
			}

			Main.getLogger().debug(5, "message-ids resolved to msg_ids = " + message_id_to_msg_id.size());
			Main.getLogger().debug(5, "message_ids resolved from in-reply-to = " + msg_id_to_message_id.size());

			// Store the parents and roots into the database
			Main.getLogger().log("The heuristic could resolve " + msg_id_to_msg_id.size() + " parent relations!");
			Main.getLogger().log("Storing associations found by in-reply-to heuristic in the database...");
			storeParents(msg_id_to_msg_id, connection);

		} catch (Exception e) {
			Main.getLogger().error("Error storing messages for heuristic in-reply!", e);
		}
	}


	/**
	 * This heuristic creates associations based on the references header fields
	 */
	private void heuristicReferences(List<MM2Message> messages, Connection connection) {
		// This is the msg_id ==> references-id
		Hashtable<String, String> msg_id_to_message_id = new Hashtable<String, String>();

		// This is message-id ==> msg_id
		Hashtable<String, String> message_id_to_msg_id = new Hashtable<String, String>();

		// This is child ==> parent
		Hashtable<String, String> msg_id_to_msg_id = new Hashtable<String, String>();
		Pattern messageid_pattern = Pattern.compile("<(.*?@.*?)>");

		try {
			for (MM2Message msg : messages) {

				// Step One: check whether the message has a message-id header
				// We assume that the "message-id" field is always set - at least to "" and is not null
				String h_message_id = msg.getHeaderEntry("message-id");
				if (h_message_id.length() > 2 ) {
					// We try to identify the message identifier part
					String maybeMatch = findLongestPatternMatch(h_message_id, messageid_pattern);
					if (!maybeMatch.equalsIgnoreCase("")) {
						String extracted_message_id = maybeMatch;
						String msg_id = msg.getHeaderEntry("msg_id");
						// Add the information to the reverse lookup table that we will need later
						message_id_to_msg_id.put(extracted_message_id, msg_id);
						Main.getLogger().debug(5, this, "I know that message " + msg_id + " has message-id " + extracted_message_id);
					}
				}


				// Step Two: check whether the message has an references header
				// Again we assume that the "references" field is at least "" and not null
				String h_references = msg.getHeaderEntry("references");
				String[] split_refs = h_references.split(" ");
				for (String ref : split_refs) {
					if(ref.contains(">") && ref.contains("<")) {
						// We try to identify the message identifier part
						String maybeMatch = findLongestPatternMatch(h_references, messageid_pattern);
						if (!maybeMatch.equalsIgnoreCase("")) {
							String extracted_in_reply_to_messageid = maybeMatch;
							String msg_id = msg.getHeaderEntry("msg_id");
							// Add the information into the forward lookup table
							Main.getLogger().debug(5, this, "I know that message " + msg_id + " is a reply to " + extracted_in_reply_to_messageid);
							msg_id_to_message_id.put(msg_id, extracted_in_reply_to_messageid);
						}
					}
				}
			}

			// Step Three: After we obtained the previous information, we will
			// resolve each in-reply-to id to a msg_id. so we know for each key in the
			// forward lookup table the msg_id it is a reply to:

			for (String child_msg_id : msg_id_to_message_id.keySet()) {
				String parent_msg_id = message_id_to_msg_id.get(msg_id_to_message_id.get(child_msg_id));
				// If we found an entry in the table
				if (parent_msg_id != null) {
					// Add this information to the parents table
					Main.getLogger().debug(5, this, "I know that message " + child_msg_id + " has the parent " + parent_msg_id);
					msg_id_to_msg_id.put(child_msg_id, parent_msg_id);
				}
			}

			Main.getLogger().debug(5, "message-ids resolved to msg_ids = " + message_id_to_msg_id.size());
			Main.getLogger().debug(5, "message_ids resolved from references = " + msg_id_to_message_id.size());

			// Store the parents and roots into the database
			Main.getLogger().log("The heuristic could resolve " + msg_id_to_msg_id.size() + " parent relations!");
			Main.getLogger().log("Storing associations found by references heuristic in the database...");
			storeParents(msg_id_to_msg_id, connection);

		} catch (Exception e) {
			Main.getLogger().error("Error storing messages for heuristic references!", e);
		}
	}

	/**
	 * This heuristic creates associations based on the subject and a time
	 * window Default Time window is 1 month
	 */
	private void heuristicSubject(List<MM2Message> messages, Connection connection) {

		// This is the msg_id ==> Date
		Hashtable<String, Date> msg_id_to_date = new Hashtable<String, Date>();

		// This is (original) subject ==> msg_id
		Hashtable<String, String> subject_to_msg_id = new Hashtable<String, String>();

		// This is msg_id ==> (processed) subject
		Hashtable<String, String> msg_id_to_subject = new Hashtable<String, String>();

		// This is child ==> parent
		Hashtable<String, String> msg_id_to_msg_id = new Hashtable<String, String>();

		// Capture the most commong reply patterns
		// Fw: Re: Aw: Wg:
		Pattern reply_pattern = Pattern.compile("^(\\[.*?\\] )?(([rR][eE]:)|([aA][wW]:)|([fF][wW]:)|([wW][gG]:)|([fF][wW][dD]:)|([wW][tT][rR]:)|([aA]ntwort:))(.*?)$");

		try {
			for (MM2Message msg : messages) {
				String msg_id = msg.getHeaderEntry("msg_id");
				msg_id_to_date.put(msg_id, msg.getMsg_date());
				// We assume the subject to be at least ""
				String raw_subject = msg.getSubject();
				// Determine whether the subject describes a reply or an original posting
				Matcher matcher = reply_pattern.matcher(raw_subject);
				if (matcher.matches()) {
					String stripped_subject = matcher.group(matcher.groupCount());
					Main.getLogger().debug(5, this, "I think message is a reply and the original subject is: " + stripped_subject.trim());
					// Store the information in the forward table
					msg_id_to_subject.put(msg_id, stripped_subject.trim());
				} else {
					// We think that this is not a reply - hence it must be an original posting ;-)
					subject_to_msg_id.put(raw_subject, msg_id);
					Main.getLogger().debug(5, this, "I think message is an original posting: " + raw_subject.trim());
				}
			}

			// Now we need to find parent relations by subject.
			// Still we will apply a sliding window approach using a given offset
			// to make sure, we don't capture events of people re-using old subject names

			for (String child_msg_id : msg_id_to_subject.keySet()) {
				String origSubj = msg_id_to_subject.get(child_msg_id);
				String parent_msg_id = subject_to_msg_id.get(origSubj);
				// If we found an entry in the table
				if (parent_msg_id != null) {
					// Check if the potential parent is (OFFSET) older than child
					Date d1 = msg_id_to_date.get(parent_msg_id);
					Date d2 = DateUtils.addMonths(msg_id_to_date.get(child_msg_id), OFFSET);
					if (d1.compareTo(d2) >= 0) {
						Main.getLogger().debug(5, this, "I know that message " + child_msg_id + " has the parent " + parent_msg_id);
						msg_id_to_msg_id.put(child_msg_id, parent_msg_id);
					}
				}
			}

			Main.getLogger().debug(5, "original posting subjects resolved = " + subject_to_msg_id.size());
			Main.getLogger().debug(5, "subjects resolved replys = " + msg_id_to_subject.size());

			// Store the parents and roots into the database
			Main.getLogger().log("The heuristic could resolve " + msg_id_to_msg_id.size() + " parent relations!");
			Main.getLogger().log("Storing associations found by in-reply-to heuristic in the database...");
			storeParents(msg_id_to_msg_id, connection);

		} catch (Exception e) {
			Main.getLogger().error("Error storing messages for heuristic in-reply!", e);
		}
	}



	/**
	 * This method tries to delete all associations in root and parent tables
	 * @throws SQLException if there was an error during deletion
	 */
	private void dropAssociations(Connection connection) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("DELETE FROM parent WHERE TRUE; DELETE FROM root WHERE TRUE;");
		stmt.close();
	}


   /** This method calculates all roots for the given messages in messageData
	 * and stores this information in the database
	 * @param messages a list of Messages
	 * @throws SQLException if there was an error with the database
	 */
	private void calculateRoots(List<MM2Message> messages, Connection connection) throws SQLException {
		// Needed Data Structures
		Hashtable<String, String> parentsTable = new Hashtable<String, String>();
		Hashtable<String, String> rootsTable = new Hashtable<String, String>();

		// Build up parents table form database
		String query = "SELECT * FROM parent";
		Statement stmt = connection.createStatement();
		ResultSet results = stmt.executeQuery(query);
		while (results.next()) {
			parentsTable.put(results.getString("msg_id"),results.getString("parent_id"));
		}

		Main.getLogger().debug(3, parentsTable.keySet().size()+ " parent relations are known!");

		// Calculate all roots
		for (MM2Message msg : messages) {
			String msgUID = msg.getHeaderEntry("msg_id");
			String rootUID = msgUID;
			if (parentsTable.containsKey(msgUID)) {
				Set<String> seenParents = new HashSet<String>();

				String myParent = parentsTable.get(msgUID);
				seenParents.add(myParent);
				while (myParent != null) {
					rootUID = myParent;
					myParent = parentsTable.get(myParent);
					if (seenParents.contains(myParent)) {
						Main.getLogger().log("Parents Cycle: " + rootUID + " " + myParent);
						break;
					} else {
						seenParents.add(myParent);
					}
				}
			}
			rootsTable.put(msgUID, rootUID);
		}
		Main.getLogger().log("Storing " + rootsTable.keySet().size() + " roots in database...");
		storeRoots(rootsTable, connection);
	}


}
