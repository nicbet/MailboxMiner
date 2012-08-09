package ca.queensu.cs.sail.mailboxmina2.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeUtility;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import ca.queensu.cs.sail.mailboxmina2.common.StreamToString;

public class MBoxReadTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.setProperty("mail.mime.decodetext.strict","false");
		System.setProperty("mail.mime.encodeeol.strict","true");

		String storageFolder = args[0];
		Session session = Session.getDefaultInstance(System.getProperties());

		try {
			Store store = session.getStore(new URLName("mstor:" + storageFolder));
			store.connect();

			// read messages from Inbox..
			for (Folder f : store.getDefaultFolder().list())
			{
				System.out.println("Reading folder " + f.getFullName());
				f.open(Folder.READ_ONLY);

				System.out.println("Folder contains " + f.getMessageCount() + " messages!");
				Message[] messages = f.getMessages();
				
				for (Message msg : messages) {
				System.out.println("[Message " + msg.getMessageNumber() + "]");

				// Headers
				//Enumeration<Header> e = msg.getAllHeaders();
				//while (e.hasMoreElements()) {
				//	Header h = e.nextElement();
				//	System.out.print("\t" + h.getName() + " = ");
				//	System.out.println(MimeUtility.decodeText(MimeUtility.unfold(h.getValue())));
				//}
				
				if (msg.getSubject() != null)
					System.out.println(MimeUtility.decodeText(MimeUtility.unfold(msg.getSubject())));

				// Parts
				if (msg.isMimeType("text/plain")) {
					try {
						String content = (String) msg.getContent();
						System.out.println(content.length());
					} catch (UnsupportedEncodingException e2) {
						CharsetDetector detector = new CharsetDetector();
						detector.setText(msg.getInputStream());
						CharsetMatch match = detector.detect();
						System.out.println("Charset Detector thinks the text is in encoding: " + match.getName() + " ... and is " + match.getConfidence() + " percent sure!");
						msg.setHeader("Content-Type", "text/plain; charset=" + match.getName());
						String content = (String) msg.getContent();
						System.out.println(content.length());
						
					}

				}
				System.out.println("--------------------------------------------------------------");
			}
			}





		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

}
