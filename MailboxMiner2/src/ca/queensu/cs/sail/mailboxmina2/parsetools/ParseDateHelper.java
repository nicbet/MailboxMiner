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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.MessagingException;

import ca.queensu.cs.sail.mailboxmina2.main.Main;

public class ParseDateHelper {

	public static Date getDateFromMessage(Message message, Connection con) {

		Date result = null;

		try {
			if (hasHeader("Date", message)) {
				Date msg_date = message.getSentDate();
				// If Java Mail could not parse the date ... 
				if (msg_date == null) {
					// see if the postgresql server can infer the date ...
					String datestr = message.getHeader("Date")[0];
					Main.getLogger().debug(2,"Invalid Message date supplied - letting Postgresql guess: " + datestr);
					try {
						Statement st = con.createStatement();
						ResultSet r = st.executeQuery("SELECT TIMESTAMP WITH TIME ZONE '" + datestr + "'");
						if (r.next())
							result = new Date(r.getTimestamp(1).getTime());
						r.close();
						st.close();
					} catch (SQLException e) {
						Main.getLogger().debug(2,"Error asking PostgreSQL for the date - using default date. " + e.getMessage());
					} 

				} else
					result = msg_date;
			} 
			if (result == null && hasHeader("Received", message)) {
				Main.getLogger().debug(2,"No Message date supplied - trying to infer");
				String[] prtemp = message.getHeader("Received")[0].split(";"); 
				String datestr = (prtemp.length > 0 ? prtemp[prtemp.length-1] : "");
				try {
					Statement st = con.createStatement();
					ResultSet r = st.executeQuery("SELECT TIMESTAMP WITH TIME ZONE '" + datestr + "'");
					if (r.next())
						result = new Date(r.getTimestamp(1).getTime());
				} catch (SQLException e) {
					Main.getLogger().debug(2,"Error asking PostgreSQL for the date - using default date. " + e.getMessage());
				}
			}
		} catch (MessagingException e) {
			Main.getLogger().error("Error while extracting the date", e);
		}

		// If nothing worked set default date
		if (result == null) {
			result = new Date(0);
		}
		
		return result;

	}

	private static boolean hasHeader(String name, Message message) {

		String[] result;
		try {
			result = message.getHeader(name);
		} catch (MessagingException e) {
			return false;
		}
		if (result == null)
			return false;
		if (result.length == 0)
			return false;
		else
			return true;

	}
	
	public static String getGMTDate(Date d) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(d);
		DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");  // RFC822 format!
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		return format.format(d);
	}


}
