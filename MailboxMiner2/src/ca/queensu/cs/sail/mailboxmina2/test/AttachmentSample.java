package ca.queensu.cs.sail.mailboxmina2.test;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.event.*;
import javax.activation.*;
import java.text.*;
import java.util.Calendar;

public class AttachmentSample {
	public static void main (String args[]) 
	throws Exception {
		String host ="omnimail1.omnifax.xerox.com"; 
		String username ="edi"; 
		String password ="ediedi"; 
		// Get session
		Session session = Session.getInstance(
				new Properties(), null);
		// Get the store
		Store store = session.getStore("imap");
		store.connect(host,username,password);
		// Get folder
		Folder folder = store.getFolder("INBOX/AIG/EQ");
		folder.open(Folder.READ_ONLY);
		BufferedReader reader = new BufferedReader (
				new InputStreamReader(System.in));
		// Get directory
		Message message[] = folder.getMessages();
		for (int i = message.length-1,n=message.length;n>i&&i!=-1;i--) {
			Date e = message[i].getSentDate();
			System.out.println("Message Inbox date :"+e);

			Address[] a;
			if ((a=message[i].getFrom())!=null) {
				for (int j=0;j<a.length;j++)
					System.out.println("From : " +a[j].toString());
			}
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat fmt1 = new SimpleDateFormat("yyyy-MM-dd 06:00:00" ); 
			cal.add(Calendar.DATE,-15); //set to 17 days earlier date (15-31)
			fmt1.setCalendar(cal); //set to date after 6am
			String s2 =fmt1.format(cal.getTime()); //returns yesterday's date in string format
			System.out.println("Yesterday's Date in string form before conversion :"+s2 ); 
			SimpleDateFormat fmt2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			Date d2 = fmt2.parse(s2); //to convert the date in string to date form
			System.out.println("Yesterday's date in date type is: "+d2);
			if (e.after(d2)) { 

				Object content = message[i].getContent();
				if (content instanceof Multipart) {
					System.out.println("Multipart" + i);
					if ((a=message[i].getFrom()) !=null) {
						for (int j=0;j<a.length;j++) {
							System.out.println("From : " +a[j].toString());
							String t = ((a[j].toString()).substring(3,13));
							String m = "AIG - TEST";
							if (t.equals(m)==false)
								handleMultipart((Multipart)content);
						} //end of for (int j=0)
					} //end of if(a=msg[i]) 
				} 
				else {
					System.out.println("ElseMultipart" + i);
					handlePart(message[i]);
				}
			} // end of (if e.after d2) 
			else
				System.out.println("Old message");
		} // end of for loop
		// Close connection 
		folder.close(false);
		store.close();
	} // end of main 
	public static void handleMultipart(Multipart multipart) 
	throws MessagingException, IOException {
		for (int i=0, n=multipart.getCount(); i<n; i++) {
			handlePart(multipart.getBodyPart(i));
		}
	} //end of handleMulitipart()

	public static void handlePart(Part part) 
	throws MessagingException, IOException {
		String disposition = part.getDisposition();
		String contentType = part.getContentType();
		if (disposition == null) { // When just body
			System.out.println("Null: " + contentType);
			// Check if plain
			/* if ((contentType.length() >= 10) && 
(contentType.toLowerCase().substring(
0, 10).equals("text/plain"))) {
part.writeTo(System.out);
} else { // Don't think this will happen
System.out.println("Other body: " + contentType);
part.writeTo(System.out);
}*/
		} //end of if null
		else if (disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
			System.out.println("Attachment VS: " + part.getFileName() + 
					" : " + contentType);
			saveFile(part.getFileName(), part.getInputStream());

		} else if (disposition.equalsIgnoreCase(Part.INLINE)) {
			System.out.println("Inline: " + 
					part.getFileName() + 
					" : " + contentType);
		} else { // Should never happen
			System.out.println("Other: " + disposition);
		}
	} // end of handlePart()

	public static void saveFile(String filename,
			InputStream input) {
		System.out.println(filename);
		try{
			if (filename == null) {
				filename = File.createTempFile("VSX", ".out").getName();
			}
			// Do no overwrite existing file
			filename = "/work/vsubra/edi/saxon/" + filename;
			File file = new File(filename);
			for (int i=0; file.exists(); i++) {
				file = new File(filename+i);
			}
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			BufferedInputStream bis = new BufferedInputStream(input);
			int aByte;
			while ((aByte = bis.read()) != -1) {
				bos.write(aByte);
			}
			bos.write(10);
			// bos.flush();
			bos.close();
			// bis.close();
		} // end of try()
		catch(IOException exp){
			System.out.println("IOException:" + exp); 
		}
	} //end of saveFile()
}//end of class aigemail