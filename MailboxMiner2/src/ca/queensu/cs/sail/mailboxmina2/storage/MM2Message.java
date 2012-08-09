/**
 * 
 */
package ca.queensu.cs.sail.mailboxmina2.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An MM2Message is a lightweight representation of the message information stored in a MM2 format database.
 * @author Nicolas Bettenburg
 *
 */
public class MM2Message {
	
	private Map<String, String> headers;
	private List<String> recipients;
	private String msg_sender_name;
	private String msg_sender_address;
	private String subject;
	private Date msg_date;
	private String body_text;
	
	/**
	 * Overloaded constructor
	 * @param body_text
	 * @param headers
	 * @param msg_date
	 * @param msg_sender_address
	 * @param msg_sender_name
	 * @param recipients
	 * @param subject
	 */
	public MM2Message(String body_text, Map<String, String> headers,
			Date msg_date, String msg_sender_address, String msg_sender_name,
			List<String> recipients, String subject) {
		super();
		this.body_text = body_text;
		this.headers = headers;
		this.msg_date = msg_date;
		this.msg_sender_address = msg_sender_address;
		this.msg_sender_name = msg_sender_name;
		this.recipients = recipients;
		this.subject = subject;
	}

	/**
	 * Standard constructor
	 */
	public MM2Message() {
		super();
		
		this.headers = new HashMap<String, String>();
		this.recipients = new ArrayList<String>();
	}

	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	/**
	 * @return the recipients
	 */
	public List<String> getRecipients() {
		return recipients;
	}

	/**
	 * @param recipients the recipients to set
	 */
	public void setRecipients(List<String> recipients) {
		this.recipients = recipients;
	}

	/**
	 * @return the msg_sender_name
	 */
	public String getMsg_sender_name() {
		return msg_sender_name;
	}

	/**
	 * @param msg_sender_name the msg_sender_name to set
	 */
	public void setMsg_sender_name(String msg_sender_name) {
		this.msg_sender_name = msg_sender_name;
	}

	/**
	 * @return the msg_sender_address
	 */
	public String getMsg_sender_address() {
		return msg_sender_address;
	}

	/**
	 * @param msg_sender_address the msg_sender_address to set
	 */
	public void setMsg_sender_address(String msg_sender_address) {
		this.msg_sender_address = msg_sender_address;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * @return the msg_date
	 */
	public Date getMsg_date() {
		return msg_date;
	}

	/**
	 * @param msg_date the msg_date to set
	 */
	public void setMsg_date(Date msg_date) {
		this.msg_date = msg_date;
	}

	/**
	 * @return the body_text
	 */
	public String getBody_text() {
		return body_text;
	}

	/**
	 * @param body_text the body_text to set
	 */
	public void setBody_text(String body_text) {
		this.body_text = body_text;
	}
	
	/**
	 * Add a recipient to the list of recipients
	 * @param recipient a {@link String} representing the recipient.
	 */
	public void addRecipient(String recipient) {
		this.recipients.add(recipient);
	}
	
	/**
	 * Add a header entry
	 * @param key a header key
	 * @param value a header value
	 */
	public void addHeader(String key, String value) {
		this.headers.put(key, value);
	}

	/**
	 * Check whether the headers contain a given key.
	 * @param key a key whose existence should be checked for
	 * @return true if the key exists, false otherwise.
	 */
	public boolean hasHeader(String key) {
		return this.headers.containsKey(key);
	}
	
	/**
	 * Retrieve a specified header entry.
	 * @param key the header key whose value should be retrieved.
	 * @return a {@link String} of the header value if exists, null otherwise.
	 */
	public String getHeaderEntry(String key) {
		return this.headers.get(key);
	}
}
