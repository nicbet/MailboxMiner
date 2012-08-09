package ca.queensu.cs.sail.mailboxmina2.parsetools;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import ca.queensu.cs.sail.mailboxmina2.common.HTML2Text;
import ca.queensu.cs.sail.mailboxmina2.common.StreamToString;
import ca.queensu.cs.sail.mailboxmina2.main.Main;

public class ParseBodyHelper {
	
	/**
	 * This method wraps around the findMainBodyTextWrapped method
	 * and catches any exceptions that are thrown
	 */
	public List<Properties> findMainBodyText(Message message) {
		List<Properties> result = new ArrayList<Properties>();
		try {
			result = findMainBodyTextWrapped(message);
		} catch (MessagingException e) {
			Main.getLogger().error("Messaging Error while finding main body", e);
		} catch (IOException e) {
			Main.getLogger().error("IO Error while finding main body", e);
		}
		return result;
	}
	
	/**
	 * This method implements different strategies to find the main body
	 * part of an electronic message.
	 * @param message the electronic message to find the main body part of
	 * @return a List of textual informations of the body/body parts that make the primary body
	 * @throws MessagingException
	 * @throws IOException
	 */
	private List<Properties> findMainBodyTextWrapped(Message message) throws MessagingException, IOException {
		List<Properties> result = new ArrayList<Properties>();
		boolean processed = false;
		
		if (message.isMimeType("text/plain")) {
			String ct = message.getContentType().toLowerCase().replaceAll("\"", "");
			if (ct.contains("charset=")) {
				if ( ct.contains("charset=us-ascii") || ct.matches("(?s)(^.*?charset=iso-8859-1$)|(^.*?charset=iso-8859-1[^0-9].*)")) {
					Main.getLogger().debug(3, "Main Body is in standard encoding!");
				} else if (ct.contains("charset=utf-8")) {
					Main.getLogger().debug(3, "Main Body is already in UTF-8 encoding!");
				} else {
					Main.getLogger().debug(3, "Main Body is NOT in standard encoding! Converting to UTF-8!");
				}
			}
			
			Properties tempResult = unpackPart(message);
			result.add(tempResult);
			processed = true;
		}
		
		if (message.isMimeType("text/html")) {
			Properties tempResult = unpackPart(message);
			result.add(tempResult);
			processed = true;
		}
		
		if (message.isMimeType("multipart/mixed") || message.isMimeType("multipart/signed")) {
			Multipart parts =(Multipart) message.getContent();
			Part firstPart = parts.getBodyPart(0);
			if (firstPart.isMimeType("text/*")) {
				Properties tempResult = unpackPart(firstPart);
				result.add(tempResult);
				processed = true;
			} else if (firstPart.isMimeType("multipart/*")) {
				List<Properties> tempResult = unpackMultipart((Multipart) firstPart.getContent());
				result.addAll(tempResult);
				processed = true;
			}
		}
		
		if (message.isMimeType("multipart/alternative") || message.isMimeType("multipart/related")) {
			Multipart parts = (Multipart) message.getContent();
			for (int partCount = 0; partCount < parts.getCount(); partCount++) {
				Part aPart = parts.getBodyPart(partCount);
				if (aPart.isMimeType("text/*")) {
					Properties tempResult =  unpackPart(aPart);
					result.add(tempResult);
					processed = true;
				} else if (aPart.isMimeType("multipart/*")) {
					List<Properties> tempResult = unpackMultipart((Multipart) aPart.getContent());
					result.addAll(tempResult);
					processed = true;
				}
			}
		}
		
		if (!processed && message.isMimeType("multipart/*")) {
			// For everything else: default is FIRST part is the main body.
			Multipart parts = (Multipart) message.getContent();
			Part firstPart = parts.getBodyPart(0);
			if (firstPart.isMimeType("text/*")) {
				Properties tempResult = unpackPart(firstPart);
				result.add(tempResult);
				processed = true;
			} else if (firstPart.isMimeType("multipart/*")) {
				List<Properties> tempResult = unpackMultipart((Multipart) firstPart.getContent());
				result.addAll(tempResult);
				processed = true;
			}
		}
		
		// Output the MIME Type of the main body part for statistics
		String msg_mime_type = "unknown";
		if (message.getContentType() != null) {
			msg_mime_type = message.getContentType();
		}
		Main.getLogger().debug(5,"Message has MIME-TYPE: " + msg_mime_type);
		
		// Try to have at least one plain text representation ...
		boolean hasTextual = false;
		for (Properties p : result) {
			if (p.getProperty("type").equalsIgnoreCase("text/plain"))
				hasTextual = true;
		}
		// If there is no text/plain try to find an text/html part and convert it ;-)
		if (!hasTextual) {
			for (int i=0; i < result.size(); i++) {
				Properties p = result.get(i);
				if (hasTextual)
					break;
				if (p.getProperty("type").equalsIgnoreCase("text/html")) {
					HTML2Text converter = new HTML2Text();
					String body_text_plain = converter.html2Text(p.getProperty("body_text"));
					Properties converted = new Properties();
					converted.setProperty("body_text", body_text_plain);
					converted.setProperty("type", "text/plain");
					converted.setProperty("note", "converted from html");
					result.add(converted);
					hasTextual = true;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * This method is used to unpack mime multiparts
	 * @param parts a Multipart object containing all the parts
	 * @return a List of unpacked textual representations contained in the parts
	 * @throws MessagingException if something goes wrong
	 */
	private List<Properties> unpackMultipart(Multipart parts) {
		List<Properties> result = new ArrayList<Properties>();
		try {
			for (int partCount = 0; partCount < parts.getCount(); partCount++) {
				Part aPart = parts.getBodyPart(partCount);
				if (aPart.isMimeType("text/*")) {
					Properties tempResult =  unpackPart(aPart);
					result.add(tempResult);
				} else if (aPart.isMimeType("multipart/*")) {
					List<Properties> tempResult = unpackMultipart((Multipart) aPart.getContent());
					result.addAll(tempResult);
				}
			}
		} catch (MessagingException e) {
			Main.getLogger().error(this, "Error while unpacking a multipart object", e);
		} catch (IOException e) {
			Main.getLogger().error(this, "IO Error while unpacking a multipart object", e);
		}
		return result;
	}
	
	
	/**
	 * This method is used to unpack the elementary case of a Part.
	 * We support only text/plain and text/html part types.
	 * Extend this message if you want to support custom types
	 * @param part The elementary mime-message part.
	 * @return a set of properties containing the textual representation
	 * and information about the part.
	 */
	private Properties unpackPart(Part part) {
		Properties result = new Properties();
		String body_text = "";
		String type = "";
		String note = "";

		try {
			// Discover the type of the part:
			if (part.isMimeType("text/plain")) {
				type = "text/plain";
				try {
					String content = (String) part.getContent();
					body_text = content;
					note = "standard";
				} catch (UnsupportedEncodingException e2) {
					Main.getLogger().debug(3,"Unsupported Encoding - trying to infer...");
					String guessed_text = inferEncodedText(part, type);
					body_text = guessed_text;
					note = "guessed encoding";
				} catch (IOException ioe) {
					Main.getLogger().debug(3,"IO Exception, Probably invalid Content-Transfer-Encoding: " + ioe.getMessage());
					part.removeHeader("Content-Transfer-Encoding");
					try {
						String content = (String) part.getContent();
						body_text = content;
						note = "substituted cte";
					} catch (IOException e) {
						// Nothing worked, just get the thing raw ...
						try {
							String content_raw = StreamToString.convertStreamToString(part.getDataHandler().getInputStream());
							body_text = content_raw;
							note = "raw fallback";
						} catch (IOException e1) {
							// Absolutely NOTHING worked -,-
							body_text = "";
							note = "invalid";
						}
					}
				}
			} else if (part.isMimeType("text/html")) {
				type = "text/html";
				try {
					String content = (String) part.getContent();
					body_text = content;
					note = "standard";
				} catch (UnsupportedEncodingException e2) {
					Main.getLogger().debug(3,"Unsupported Encoding - trying to infer...");
					String guessed_text = inferEncodedText(part, type);
					body_text = guessed_text;
					note = "guessed encoding";
				} catch (IOException ioe) {
					Main.getLogger().debug(3,"IO Exception, Probably invalid Content-Transfer-Encoding: " + ioe.getMessage());
					part.removeHeader("Content-Transfer-Encoding");
					try {
						String content = (String) part.getContent();
						body_text = content;
						note = "substituted cte";
					} catch (IOException e) {
						// Nothing worked, just get the thing raw ...
						try {
							String content_raw = StreamToString.convertStreamToString(part.getDataHandler().getInputStream());
							body_text = content_raw;
							note = "raw fallback";
						} catch (IOException e1) {
							// Absolutely NOTHING worked -,-
							body_text = "";
							note = "invalid";
						}
					}
				}
			} 
		} catch (MessagingException e) {
			Main.getLogger().error("Error getting Content information!");
		}


		// Create result
		result.setProperty("body_text", body_text);
		result.setProperty("type", type);
		result.setProperty("note", note);
		// Return result
		return result;
	}
	
	
	/**
	 * This method is used to guess the encoding of a given input
	 * using statistical methods and byte-order heuristics.
	 * The same approach is used by Apple Mail and the Mozilla Internet Suite.
	 * @param messagepart the part of a MimeMessage to guess the encoding for
	 * @return a String containing the textual representation in the guessed encoding if possible 
	 */
	private static String inferEncodedText(Part messagepart, String type) {
		String result = null; 
		
		CharsetDetector detector = new CharsetDetector();
		try {
			detector.setText(getByteArrayFromStream(messagepart.getInputStream()));
		} catch (IOException e) {
			Main.getLogger().error("IO Error in inferEncodedText while trying to use detector", e);
		} catch (MessagingException e) {
			Main.getLogger().error("Messaging Error in inferEncodedText while trying to use detector", e);
		}
		
		CharsetMatch match = detector.detect();
		Main.getLogger().debug(3,"Charset Detector thinks the text is in encoding: " + match.getName() + " ... and is " + match.getConfidence() + " percent sure!");
		try {
			messagepart.setHeader("Content-Type", type + "; charset=" + match.getName());
			try {
				String content = (String) messagepart.getContent();
				result = content;
			} catch (IOException e) {
				Main.getLogger().error("IO Error in inferEncodedText:", e);
			}
		} catch (MessagingException e) {
			Main.getLogger().error("Messaging Error in inferEncodedText", e);
		}
		
		return result;
	}
	
	
	
	
	/**
	 * As the guessing algorithm needs mark() and reset() support in the InputStream
	 * and these features are not provided by all implementations of the different
	 * part types in Java Mail API we first encode the InputStream in a byte array
	 * @param in an InputStream
	 * @return a byte array containing the contents of the InputStream
	 */
	private static byte[] getByteArrayFromStream(InputStream in) {
		ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(ba_out);
			byte[] buf = new byte[8192];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} catch ( IOException e) { 
		} finally {
				// close streams, but don't mask original exception, if any
				try {
					
					if (in != null)
						in.close();
				} catch (IOException ex) { }
				try {
					if (out != null)
						out.close();
					ba_out.close();
				} catch (IOException ex) { }
		}
		
		return  ba_out.toByteArray();
	}
	
	
	
}
