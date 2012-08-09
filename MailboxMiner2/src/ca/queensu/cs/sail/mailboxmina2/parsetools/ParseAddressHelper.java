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

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

import ca.queensu.cs.sail.mailboxmina2.common.EmailAddress;
import ca.queensu.cs.sail.mailboxmina2.main.Main;

public class ParseAddressHelper {
	
	
	/**
	 * Big ugly code to try to find the sender's name and address from a MimeMessage
	 * @param message the message form which to extract this information
	 * @return a Properties set containing the name and address
	 */
	public static Properties inferFromAddress(Message message) {
		String msg_sender_name = "";
		String msg_sender_address = "";
		
		try {
			// Get FROM address parts
			Address[] fromaddresses = message.getFrom();
			InternetAddress backupaddr = new InternetAddress("\"Unknown Name\" <unknown@address.com>");
			if (fromaddresses != null) {
				if (fromaddresses.length > 0) {
					InternetAddress fromaddr = (InternetAddress) fromaddresses[0];
					msg_sender_name = (fromaddr.getPersonal() == null ? "" : fromaddr.getPersonal());
					msg_sender_address = (fromaddr.getAddress() == null ? "" : fromaddr.getAddress());
				}
			}
			if (msg_sender_name.equalsIgnoreCase("") || msg_sender_address.equalsIgnoreCase("")) {

				// Backup: try to get them from the Received Header ...
				if (msg_sender_name.equalsIgnoreCase("")) {
					Main.getLogger().debug(2,"No Sender Name supplied");
					msg_sender_name = "Unkown Name";
				} 
				if (msg_sender_address.equalsIgnoreCase("")) {
					Main.getLogger().debug(2,"No Sender Address supplied - trying to infer!");
					String[] received = message.getHeader("Received");
					if (received != null) {
						if (received.length > 0) {
							String foundAddr = EmailAddress.findFromInput(MimeUtility.unfold(received[0]));
							if (!foundAddr.equalsIgnoreCase("")) {
								backupaddr = new InternetAddress(foundAddr);
								msg_sender_address = backupaddr.getAddress();
								Main.getLogger().debug(2,"Inferred Address successfully!");
							} else {
								Main.getLogger().debug(2,"Could not infer address - using fallback!");
								msg_sender_address = "unkown@address.com";
							}
						}
					}
				}
			}

		} catch (MessagingException e) {
			Main.getLogger().error("JavaMail Error", e);
		}
		
		Properties result = new Properties();
		result.setProperty("msg_sender_name", msg_sender_name);
		result.setProperty("msg_sender_address", msg_sender_address);
		
		return result;
	}

}
