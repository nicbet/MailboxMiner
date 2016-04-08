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
package ca.queensu.cs.sail.mailboxmina2.parsetools;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import ca.queensu.cs.sail.mailboxmina2.common.Attachment;
import ca.queensu.cs.sail.mailboxmina2.main.Main;

public class ParseAttachmentsHelper {
	
	public static List<Attachment> getAllAttachmentsIn(Message message) {
		List<Attachment> attachments = new ArrayList<Attachment>();
		try {
			Object content = message.getContent();
			if (content instanceof Multipart) {
				List<Attachment> atts = handleMultiPart((Multipart)content);
				attachments.addAll(atts);
			} else if (content instanceof String) {
				// Do nothing this is a main Body
			} else {
				Attachment att = handlePart(message);
				attachments.add(att);
			}
		} catch (MessagingException e) {
			Main.getLogger().error("Messaging Error while extracting attachments from Message!", e);
		} catch (IOException e) {
			Main.getLogger().error("IO Error while extracting attachments from Message!", e);
		}
		return attachments;
	}
			
	/**
	 * Elemental case
	 * @param part an elemental part to handle
	 * @return this part as an attachment, if it is an attachment
	 */
	private static Attachment handlePart(Part part) throws MessagingException, IOException {
		String content_type = part.getContentType();
		String filename = (part.getFileName() == null ? "output.dat" : part.getFileName());
		
		ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
		OutputStream out = null;
		InputStream in = null;
		try {
			out = new BufferedOutputStream(ba_out);
			in = part.getInputStream();
			byte[] buf = new byte[8192];
			int len;
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len); 
		} finally {
			// close streams, but don't mask original exception, if any
			try {
				if (in != null)
					in.close();
			} catch (IOException ex) { }
			try {
				if (out != null)
					out.close();
			} catch (IOException ex) { }
		}
		Attachment att = new Attachment(ba_out.toByteArray(), content_type, filename);
		
		return att;
	}
	
	/**
	 * Handle all the parts in the multipart ...
	 * @param multipart a multipart
	 * @return a list of attachments of the parts in the multipart
	 */
	private static List<Attachment> handleMultiPart(Multipart multipart) throws MessagingException, IOException {
		List<Attachment> attachments = new ArrayList<Attachment>();
		for (int i=0; i < multipart.getCount(); i++) {
			BodyPart possiblePart = multipart.getBodyPart(i);
			if (possiblePart.isMimeType("multipart/*")) {
				List<Attachment> unwrappedAtts = handleMultiPart((Multipart)possiblePart.getContent());
				attachments.addAll(unwrappedAtts);
			} else {
				Attachment possibleAttachment = handlePart(multipart.getBodyPart(i));
				if (possibleAttachment != null)
					attachments.add(possibleAttachment);
			}
		}
		return attachments;
	}
	
}
