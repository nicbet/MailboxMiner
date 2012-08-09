package ca.queensu.cs.sail.mailboxmina2.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.internet.MimeUtility;

import ca.queensu.cs.sail.mailboxmina2.common.MailboxParser;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;


public class MBoxReadTest3 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String storageFolder = args[0];
		MailboxParser p = new MailboxParser();
		List<Message> messages = p.parseMessages(storageFolder);
		
		try {
			int i=0;
			for (Message msg : messages) {
				
				i++;
				System.out.println("----------------------------------------------------------");
				System.out.println("MESSAGE " + i);

				// Headers
				Enumeration<Header> e = msg.getAllHeaders();
				while (e.hasMoreElements()) {
					Header h = e.nextElement();
					System.out.print("\t" + h.getName() + " = ");
					System.out.println(MimeUtility.decodeText(MimeUtility.unfold(h.getValue())));
				}
	
				if (msg.getSubject() != null)
					System.out.println(MimeUtility.decodeText(MimeUtility.unfold(msg.getSubject())));
				else
					System.out.println("NULLHEADER!!");
				// Parts
				if (msg.isMimeType("text/plain")) {
					try {
						String content = (String) msg.getContent();
						System.out.println(content);
					} catch (UnsupportedEncodingException e2) {
						CharsetDetector detector = new CharsetDetector();
						detector.setText(msg.getInputStream());
						CharsetMatch match = detector.detect();
						System.out.println("Charset Detector thinks the text is in encoding: " + match.getName() + " ... and is " + match.getConfidence() + " percent sure!");
						msg.setHeader("Content-Type", "text/plain; charset=" + match.getName());
						String content = (String) msg.getContent();
						System.out.println(content);
					} catch (IOException ioe) {
						msg.removeHeader("Content-Transfer-Encoding");
						String content = (String) msg.getContent();
						System.out.println(content);
						
					}
				} else {
					Object content = msg.getContent();
					if (content instanceof InputStream) {	// Could not determine content-type
						// System.out.println(StreamToString.convertStreamToString((InputStream) content));
					} else {	// multipart
						
					}
					System.out.println("==== END OF NON PLAIN TEXT ====");
				}
				System.out.println("--------------------------------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}
