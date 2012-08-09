package ca.queensu.cs.sail.mailboxmina2.test;

import ca.queensu.cs.sail.mailboxmina2.common.MailboxParser;



public class MBoxReadTest2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String storageFolder = args[0];
		MailboxParser p = new MailboxParser();
		p.parseMessages(storageFolder);
		
	}
}
