package ca.queensu.cs.sail.mailboxmina2.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.*;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import ca.queensu.cs.sail.mailboxmina2.main.Main;



/**
 * Parser for .mbox files. Mimics the heuristics used by the UNIX formail program to cope with bogus input.
 * @author Nicolas Bettenburg
 *
 */
public class MailboxParser {

	/**
	 * Minimum amount of consecutive header lines to encounter after a potential separator line before it
	 * is considered for splitting.
	 */
	private int HEADERTHRESHOLD = 2;
	
	/**
	 * Counts the number of collisions in the HashTable of seen messages. (I.e. counts duplicate e-mails)
	 */
	private int collisions;
	
	/**
	 * Set whether to discard duplicate messages or not. Default behaviour is to silently get rid of duplicates.
	 */
	private boolean IGNOREDUPLICATES = true;

	public int getCollisions() {
		return collisions;
	}

	public void setCollisions(int collisions) {
		this.collisions = collisions;
	}

	public boolean isIGNOREDUPLICATES() {
		return IGNOREDUPLICATES;
	}

	public void setIGNOREDUPLICATES(boolean ignoreduplicates) {
		IGNOREDUPLICATES = ignoreduplicates;
	}

	public int getHEADERTHRESHOLD() {
		return HEADERTHRESHOLD;
	}

	public void setHEADERTHRESHOLD(int headerthreshold) {
		HEADERTHRESHOLD = headerthreshold;
	}

	/**
	 * Parse a given .mbox file and return a list of {@link Message} objects.
	 * @param filename a {@link String} containing the path to an .mbox file.
	 * @return a {@link List} of {@link Message} objects extracted from the file.
	 */
	public List<Message> parseMessages(String filename) {
		Logger logger = Main.getLogger();
		
		collisions = 0;
		List<Message> messages = new ArrayList<Message>();
		HashSet<Integer> seenMessages = new HashSet<Integer>();

		// Open the file for reading
		try {
			StringBuilder inputBuilder = new StringBuilder();
			String line = "";
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			// Read the mbox file line by line
			while ((line = reader.readLine()) != null) {
				inputBuilder.append(line);
				inputBuilder.append(System.getProperty("line.separator"));
			}

			String text = inputBuilder.toString();
			inputBuilder = null;

			String[] rawlines = text.split("(\n\r)|(\n)|(\r)");
			logger.debug(5,this, "Split file into " + rawlines.length + " lines");

			Pattern seperatorPattern = Pattern.compile("^From (.*?) (.*?):(.*?):(.*?)$");		// From Apache JAMES server
			Pattern headerPattern = Pattern.compile("^[\\x21-\\x39\\x3B-\\x7E]+:(.*)$");		// From RFC 5322 - Oct 2008
			String ssep = System.getProperty("line.separator");

			// Here comes the big ugly loop ...
			int lastFoundSepLine = -1;
			Map<Integer, Integer> separatorsMap = new HashMap<Integer, Integer>();

			for (int line_num=0; line_num < rawlines.length; line_num++) {

				String currentLine = rawlines[line_num];

				// If we found a header name line
				if (headerPattern.matcher(currentLine).matches()) {
					logger.debug(6,this, "HEADER MATCH! " + line_num);
					if (lastFoundSepLine != -1) {
						if (separatorsMap.containsKey(lastFoundSepLine)) {
							int numHeaders = separatorsMap.get(lastFoundSepLine);
							numHeaders++;
							separatorsMap.put(lastFoundSepLine, numHeaders);
						}
					}
				}

				// If we found a separator line 
				if (seperatorPattern.matcher(currentLine).matches()) {
					logger.debug(6,this, "SEP MATCH! " + line_num);
					lastFoundSepLine = line_num;
					separatorsMap.put(lastFoundSepLine, 0);
				}
			}
			// Treat the end of the file as potential separator ;-)
			separatorsMap.put(rawlines.length, HEADERTHRESHOLD);


			// Compose the messages 
			// If we read at least HEADERTHRESHOLD many headers after the separator
			List<Integer> separators = new ArrayList<Integer>();
			for (Integer x : separatorsMap.keySet()) {
				if (separatorsMap.get(x) >= HEADERTHRESHOLD) {
					separators.add(x);
				} else {
					// Line x is a bogus header line and should be escaped!!
					rawlines[x.intValue()] = ">" + rawlines[x.intValue()];
				}
			}

			Collections.sort(separators);

			for (int i=0; i < separators.size()-1; i++) {
				int startLine = separators.get(i);
				int endLine = separators.get(i+1);
				logger.debug(6,this, "Message from lines " + startLine + " - " + endLine);
				// compose a raw message
				StringBuilder rawMsgBuilder = new StringBuilder();
				for (int l = startLine+1; l < endLine; l++) {
					rawMsgBuilder.append(rawlines[l] + ssep);
				}
				String rawMessageText = rawMsgBuilder.toString().trim();
				int hashKey = rawMessageText.hashCode();
				if (!seenMessages.contains(hashKey)) {
					messages.add(convertTextToMimeMessage(rawMessageText));
					if (IGNOREDUPLICATES)
						seenMessages.add(hashKey);
				} else {
					collisions++;
				}
			}			
			// end compose the last message if that one was valid

			logger.debug(3,this, "Split into " + messages.size() + " messages!");
			
			
		} catch (IOException e) {
			logger.debug(3,"Error while trying to read file: " + filename);
			logger.debug(3,e.getMessage());
			logger.debug(3,"-------- Stacktrace ----------");
			logger.error("Exception", e);
		}
		return messages;
	}


	/**
	 * Parse a text block as an email and convert it into a mime message
	 * @param emailBody The headers and body of an email. This will be parsed into a mime message and stored
	 */
	private MimeMessage convertTextToMimeMessage(String emailBody) {
		//this.emailBody = emailBody;
		MimeMessage mimeMessage = null;
		// Parse the mime message as we have the full message now (in string format)
		ByteArrayInputStream mb = new ByteArrayInputStream(emailBody.getBytes());
		Properties props = System.getProperties();
		Session session = Session.getDefaultInstance(props);
		try {
			mimeMessage = new MimeMessage(session, mb);

		} catch (MessagingException e) {
			Main.getLogger().error("Error converting raw message to MimeMessage", e);
		}

		/*
        String toAddr = null;
        try {
            // Attempt to read the TO field and see if it errors
            toAddr = mimeMessage.getRecipients(javax.mail.Message.RecipientType.TO).toString();
        } catch (Exception e) {
            // It has errored, so time for plan B
            // use the from field I suppose
            try {
                mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, mimeMessage.getFrom());
                if (getLogger().isDebugEnabled()) {
                    StringBuffer logBuffer =
                            new StringBuffer(128)
                            .append(this.getClass().getName())
                            .append(" Patching To: field for message ")
                            .append(" with  From: field");
                    getLogger().debug(logBuffer.toString());
                }
            } catch (MessagingException e1) {
                getLogger().error("Unable to set to: field to from: field", e);
            }
        } */
		return mimeMessage;
	}


}
