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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

import ca.queensu.cs.sail.mailboxmina2.common.Attachment;
import ca.queensu.cs.sail.mailboxmina2.main.Main;
import ca.queensu.cs.sail.mailboxmina2.main.ressources.Ressources;
import ca.queensu.cs.sail.mailboxmina2.parsetools.ParseAddressHelper;
import ca.queensu.cs.sail.mailboxmina2.parsetools.ParseAttachmentsHelper;
import ca.queensu.cs.sail.mailboxmina2.parsetools.ParseBodyHelper;
import ca.queensu.cs.sail.mailboxmina2.parsetools.ParseDateHelper;

/**
 * The ConnectorPSQL class is a {@link IConnector} style class.<br>
 * It acts as a bridge between the program and the data layer (i.e. the PostgreSQL database).
 * @author Nicolas Bettenburg
 *
 */
public class ConnectorPSQL {


	private Connection connection = null;
	private Properties props = null;

	/**
	 * Getter
	 * @return the properties of the Connector
	 */
	public Properties getProps() {
		return props;
	}

	/**
	 * Setter
	 * @param props the props for the Connector
	 */
	public void setProps(Properties props) {
		this.props = props;
	}

	/**
	 * Constructor
	 * @param properties to set the {@link Properties} of the Connector.<br>
	 * The following properties are required:<br>
	 * <ul>
	 * <li> <b>db_url</b> is the jdbc connection {@link String} including the database name, e.g., //server/databasename
	 * <li> <b>username</b> is the username required to connect to the database.
	 * <li> <b>password</b> is the password required to connect to the database.
	 * </ul>
	 */
	public ConnectorPSQL(Properties properties) {
		this.props = properties;
	}


	/**
	 * Connect to the database if not done before and return the {@link Connection} object.
	 * @return the {@link Connection} to the database if successful, null otherwise.
	 */
	public Connection getConnection() {
		if (connection != null)
			return connection;

		// try to load the PostgreSQL Database JDBC driver
		try {
			Class.forName("org.postgresql.Driver"); // load the driver
		} catch (ClassNotFoundException e) {
			Main.getLogger().error(this,"Unable to load PostgreSQL JDBC driver!",e);
			return null;
		}

		String db_url = props.getProperty("db_url");
		String username = props.getProperty("username");
		String password = props.getProperty("password");

		try {
			// connect to the db
			Connection connection = DriverManager.getConnection("jdbc:postgresql:" + db_url, username, password);
			// get MetaData to confirm connection
			DatabaseMetaData dbmd = connection.getMetaData();
			Main.getLogger().log("Connection to " + dbmd.getDatabaseProductName() + " " + dbmd.getDatabaseProductVersion() + " successful!");
			this.connection = connection;
			return connection;
		} catch (SQLException e) {
			Main.getLogger().error(this,"Error while opening the database connection!",e);
			return null;
		}
	}


	/**
	 * Generate SQL calls to store a Java Mail API {@link Message} in an MM2 format database.<br>t
	 * For more information on the MM2 format please read the documentation.
	 * @param message the {@link Message} object to be stored in the database
	 * @param statement a {@link Statement} that has been prepared previously to allow for batch insertion of statements.
	 * @return true when the SQL statements for the Message could successfully be generated and added, false otherwise.
	 */
	@SuppressWarnings("unchecked")
	public boolean storeMessage(Message message, Statement statement) {
		boolean success = true;

		if (connection == null) {
			Main.getLogger().warning("Connection is not open - trying to establish connection first...");
			if (getConnection() == null) {
				Main.getLogger().error("Could not establish connection!");
				success = false;
			}
		}

		// Split, extract, process and store ;-)
		// Warning stupid, ugly, boring, long, awful code following!

		// Start with the messages table  ------------------------------------------------------------------------------------------------------

		// First the Sender's Name and Address
		Properties fromParseResult = ParseAddressHelper.inferFromAddress(message);
		String msg_sender_name = fromParseResult.getProperty("msg_sender_name");
		String msg_sender_address = fromParseResult.getProperty("msg_sender_address");

		// Then the message's date
		Date messageDate = ParseDateHelper.getDateFromMessage(message, connection);
		String msg_date = ParseDateHelper.getGMTDate(messageDate);

		// Then the message's subject

		String msg_subject = "";
		try {
			msg_subject = (message.getSubject() != null ? message.getSubject() : "");
		} catch (MessagingException e) {
			Main.getLogger().error(this, "Could not retrieve the subject for the message", e);
			success = false;
		}


		// Now write the SQL to the statement
		Main.getLogger().debug(1,this, msg_sender_address + " " + msg_sender_name);
		Main.getLogger().debug(1,this, msg_date);
		Main.getLogger().debug(1,this, msg_subject);
		Main.getLogger().debug(1,this, "");

		StringBuilder msgSqlStringBuilder = new StringBuilder();
		msgSqlStringBuilder.append("INSERT INTO messages (msg_sender_name, msg_sender_address, msg_date, msg_subject) VALUES (");
		msgSqlStringBuilder.append("E'" + escapeString(msg_sender_name) + "'");
		msgSqlStringBuilder.append(",");
		msgSqlStringBuilder.append("E'" + escapeString(msg_sender_address) + "'");
		msgSqlStringBuilder.append(",");
		msgSqlStringBuilder.append("TIMESTAMP WITH TIME ZONE '");
		msgSqlStringBuilder.append(msg_date);
		msgSqlStringBuilder.append("',");
		msgSqlStringBuilder.append("E'" + escapeString(msg_subject) + "'");
		msgSqlStringBuilder.append(");");
		// Store this information!
		try {
			statement.addBatch(msgSqlStringBuilder.toString());
		} catch (SQLException e) {
			Main.getLogger().error(this, "Could not add message to batch statement!", e);
		}

		// Continue with the Headers ------------------------------------------------------------------------------------------------------
		try {
			Enumeration allheaders = message.getAllHeaders();
			while (allheaders.hasMoreElements()) {
				Header aheader = (Header) allheaders.nextElement();
				statement.addBatch("INSERT INTO headers (msg_id, header_key, header_value) VALUES ("
						+ "currval( 'messages_msg_id_seq'),E'"
						+ escapeString(aheader.getName())
						+ "',E'" + escapeString(MimeUtility.unfold(aheader.getValue()))
						+ "');");
			}
		} catch (MessagingException e) {
			Main.getLogger().error(this, "Could not get all headers", e);
			success = false;
		} catch(SQLException e) {
			Main.getLogger().error(this, "Could not add message to batch statement!", e);
			success = false;
		}


		// Now the recipients ----------------------------------------------------------------------------------------------------------------
		// First primary recipients
		try {
			Address[] recipients = null;
			try {
				recipients = message.getRecipients(Message.RecipientType.TO);
			} catch (MessagingException e) {
				Main.getLogger().error(this, "Could not get primary recipients!", e);
				success = false;
			}
			if (recipients != null) {
				for (Address a : recipients) {
					if (a instanceof InternetAddress ) {
						InternetAddress recipient = (InternetAddress) a;
						String recp_name = (recipient.getPersonal() == null ? "Unknown Name" : recipient.getPersonal());
						String recp_adr = (recipient.getAddress() == null ? "unknown@address.com" : recipient.getAddress());

						statement.addBatch("INSERT INTO recipients (msg_id, recipient_type, recipient_name, recipient_address) VALUES ("
								+ "currval( 'messages_msg_id_seq'),E'"
								+ escapeString("TO") + "',"
								+ "E'" + escapeString(recp_name) + "',"
								+ "E'" + escapeString(recp_adr) + "');");
					}
				}
			} else {
				String recp_name = "Unknown Name";
				String recp_adr = "unknown@address.com";
				statement.addBatch("INSERT INTO recipients (msg_id, recipient_type, recipient_name, recipient_address) VALUES ("
						+ "currval( 'messages_msg_id_seq'),E'"
						+ escapeString("TO") + "',"
						+ "E'" + escapeString(recp_name) + "',"
						+ "E'" + escapeString(recp_adr) + "');");
			}
		} catch(SQLException e) {
			Main.getLogger().error(this, "Could not add recipients to batch statement!", e);
			success = false;
		}
		// Second carbon copy recipients
		try {
			Address[] recipients = message.getRecipients(Message.RecipientType.CC);
			if (recipients != null) {
				for (Address a : recipients) {
					if (a instanceof InternetAddress ) {
						InternetAddress recipient = (InternetAddress) a;
						String recp_name = (recipient.getPersonal() == null ? "Unknown Name" : recipient.getPersonal());
						String recp_adr = (recipient.getAddress() == null ? "unknown@address.com" : recipient.getAddress());

						statement.addBatch("INSERT INTO recipients (msg_id, recipient_type, recipient_name, recipient_address) VALUES ("
								+ "currval( 'messages_msg_id_seq'),E'"
								+ escapeString("CC") + "',"
								+ "E'" + escapeString(recp_name) + "',"
								+ "E'" + escapeString(recp_adr) + "');");
					}
				}
			}
		} catch (MessagingException e) {
			Main.getLogger().error(this, "Could not get carbon copy recipients!", e);
			success = false;
		} catch(SQLException e) {
			Main.getLogger().error(this, "Could not add recipients to batch statement!", e);
			success = false;
		}

		// Last Blind Copy recipients
		try {
			Address[] recipients = message.getRecipients(Message.RecipientType.BCC);
			if (recipients != null) {
				for (Address a : recipients) {
					if (a instanceof InternetAddress ) {
						InternetAddress recipient = (InternetAddress) a;
						String recp_name = (recipient.getPersonal() == null ? "Unknown Name" : recipient.getPersonal());
						String recp_adr = (recipient.getAddress() == null ? "unknown@address.com" : recipient.getAddress());

						statement.addBatch("INSERT INTO recipients (msg_id, recipient_type, recipient_name, recipient_address) VALUES ("
								+ "currval( 'messages_msg_id_seq'),E'"
								+ escapeString("BCC") + "',"
								+ "E'" + escapeString(recp_name) + "',"
								+ "E'" + escapeString(recp_adr) + "');");
					}
				}
			}
		} catch (MessagingException e) {
			Main.getLogger().error(this, "Could not get blind copy recipients!", e);
			success = false;
		} catch(SQLException e) {
			Main.getLogger().error(this, "Could not add recipients to batch statement!", e);
			success = false;
		}


		// Now we get to the Primary Bodies .... -----------------------------------------------------------------------------------------
		ParseBodyHelper bodyParser = new ParseBodyHelper();
		List<Properties> body_text_parse_results = bodyParser.findMainBodyText(message);
		for (Properties body_text_parse_result : body_text_parse_results) {
			String body_type = body_text_parse_result.getProperty("type");
			String body_note = body_text_parse_result.getProperty("note");
			String body_text = body_text_parse_result.getProperty("body_text");

			Main.getLogger().debug(1, this, "Body Type: " + body_type);
			Main.getLogger().debug(1, this, "Body Decode Note: " + body_note);

			try {
				statement.addBatch("INSERT INTO bodies (msg_id, body_txt, body_type, note) VALUES ("
						+ "currval( 'messages_msg_id_seq'),E'"
						+ escapeString(body_text) + "',"
						+ "E'" + escapeString(body_type) + "',"
						+ "E'" + escapeString(body_note) + "');");
			} catch (SQLException e) {
				Main.getLogger().error(this, "Could not add body to batch statement!", e);
				success = false;
			}
		}


		// Last thing to do: save all the attachments ...
		List<Attachment> attachments = ParseAttachmentsHelper.getAllAttachmentsIn(message);
		int i=0;
		for (Attachment att : attachments) {
			i++;
			try {
				statement.addBatch("INSERT INTO attachments (msg_id, mime_type, data, order_id, size) VALUES ("
						+ "currval( 'messages_msg_id_seq'),E'"
						+ escapeString(att.getMime_type()) + "',"
						+ "'" + toPGString(att.getData()) + "',"
						+  Integer.toString(i) + ", "
						+  Integer.toString(att.getData().length) +   ");");
				Main.getLogger().debug(1, att.getMime_type() + ", " + att.getOriginal_filename() + ", " + att.getData().length);
			} catch (SQLException e) {
				Main.getLogger().error(this, "Could not add attachment to batch statement!", e);
				success = false;
			}
		}

		Main.getLogger().putSeparator("debug");
		return success;
	}

	/**
	 * Helper method to make a given input save for usage in an SQL statement.
	 * @param input a {@link String} to be made SQL safe.
	 * @return a {@link String} with escaped ' and \ characters.
	 */
	private String escapeString(String input) {
		String escaped = input;
		escaped = escaped.replaceAll("\\\\", "\\\\\\\\");
		escaped = escaped.replaceAll("\'", "\\\\\'");
		// Last Try to substitute all 0x00 (null) bytes from the string with 0x20 (space)
		StringBuilder ccc = new StringBuilder();
		for (int i=0; i < escaped.length(); i++) {
			if (escaped.codePointAt(i) != 0x00 ) {
				ccc.append(escaped.charAt(i));
			}
		}
		escaped = ccc.toString();
		return escaped;
	}

	/**
	 * Helper method to make a given input save for usage in an SQL statement.<br>
	 * This function does not substitute the 0x00 characters and should be used for attachments.
	 * @param input a {@link String} to be made SQL safe.
	 * @return a {@link String} with escaped ' and \ characters.
	 */
	private String escapeStringNotNull(String input) {
		String escaped = input;
		escaped = escaped.replaceAll("\\\\", "\\\\\\\\");
		escaped = escaped.replaceAll("\'", "\\\\\'");
		return escaped;
	}

	/**
	 * Helper method to convert byte[] data to an Octet String.<br>
	 * The standard approach is to used JDBC's setByte() methods.<br>
	 * However - sometime we want to specify the data in a batch of SQL statements,
	 * especially when we rely on a batch of statements that keep track of auto-incrementing
	 * serials in different tables (we cannot use PreparedStatements).
	 * @param p_buf a byte array holding the data.
	 * @return a {@link String} containing the Octet String representation of the data.
	 */
	public static String toPGString(byte[] p_buf) {
		if(p_buf==null)
			return null;
		StringBuffer l_strbuf = new StringBuffer();
		for (int i = 0; i < p_buf.length; i++) {
			int l_int = (int)p_buf[i];
			if (l_int < 0) {
				l_int = 256 + l_int;
			}
			//we escape the same non-printable characters as the back-end
			//we must escape all 8bit characters otherwise when converting
			//from java unicode to the db character set we may end up with
			//question marks if the character set is SQL_ASCII
			//escape charcter with the form \000, but need two \\ because of
			//the parser
			l_strbuf.append("\\\\");
			l_strbuf.append((char)(((l_int >> 6) & 0x3)+48));
			l_strbuf.append((char)(((l_int >> 3) & 0x7)+48));
			l_strbuf.append((char)((l_int & 0x07)+48));

		}
		return l_strbuf.toString();
	}

	/**
	 * Read database contents and return a list of lightweight MM2Message objects, which represent elements
	 * suitable for reconstruction of threads.
	 * @param allHeaders a boolean value indicating whether all headers should be retrieved.
	 * @param withMainBody a boolean value indicating whether the main body text should be retrieved.
	 * @return
	 */
	public List<MM2Message> getMM2Messages(boolean allHeaders, boolean withMainBody) {
		List<MM2Message> messages = new ArrayList<MM2Message>();

		if (connection == null) {
			Main.getLogger().warning("Connection not established ... trying to open connection first.");
			Connection connectionAttemt = getConnection();
			if (connectionAttemt == null) {
				Main.getLogger().error("Could not establish connection!");
			}
		}
		String query = "";
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(Ressources.class.getResource("MM2MessagesThreadQuery.sql").openStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while((line = reader.readLine())!= null)
				sb.append(line + System.getProperty("line.separator"));
			reader.close();
			query = sb.toString();
		} catch (Exception e) {
			Main.getLogger().error("Critical Error - Ressource not found!", e);
			System.exit(1);
		}

		// The following code has heavy dependencies on the database layout and the file MM2MessagesThreadQuery.sql
		// should refactor this ugly hack some time... but not today
		try {
			Statement statement = connection.createStatement();
			ResultSet queryResults = statement.executeQuery(query);
			while (queryResults.next()) {
				MM2Message msg = new MM2Message();

				msg.setMsg_sender_name(queryResults.getString("msg_sender_name"));
				msg.setMsg_sender_address(queryResults.getString("msg_sender_address"));

				msg.setMsg_date(new Date(queryResults.getTimestamp("msg_date").getTime()));
				msg.setSubject((queryResults.getString("msg_subject") == null ? "" : queryResults.getString("msg_subject")));

				msg.addHeader("in-reply-to", (queryResults.getString("inreply") == null? "" : queryResults.getString("inreply")));
				msg.addHeader("message-id", (queryResults.getString("messageid") == null? "" : queryResults.getString("messageid")));
				msg.addHeader("references", (queryResults.getString("references") == null ? "" : queryResults.getString("references")));
				msg.addHeader("msg_id", queryResults.getString("msg_id"));

				messages.add(msg);
			}
		} catch (SQLException e) {
			Main.getLogger().error(this, "Error receiving messages from database ", e);
		}

		return messages;
	}

}
