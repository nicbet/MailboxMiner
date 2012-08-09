package ca.queensu.cs.sail.mailboxmina2.common;

/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 *                                                              *
 * modified by Nicolas Bettenburg                               *
 * (c) 2009                                                     *
 * MboxRepository is a READ-ONLY MimeMessage Provider           *
 ****************************************************************/


import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

/**
 * Implementation of a MailRepository using UNIX mbox files.
 *
 * <p>Requires a configuration element in the .conf.xml file of the form:
 *  <br>&lt;repository destinationURL="mbox://&lt;directory&gt;"
 *  <br>            type="MAIL"
 *  <br>&lt;/directory&gt; is where the individual mbox files are read from/written to
 * <br>Type can ONLY be MAIL (SPOOL is NOT supported)
 *
 * <p>Requires a logger called MailRepository.
 *
 * <p> Implementation notes:
 * <p>
 * This class keeps an internal store of the mbox file
 * When the internal mbox file is updated (added/deleted)
 * then the file will be re-read from disk and then written back.
 * This is a bit inefficent but means that the file on disk
 * should be correct.
 * <p>
 * The mbox store is mainly meant to be used as a one-way street.
 * Storing new emails is very fast (append to file) whereas reading them (via POP3) is
 * slower (read from disk and parse).
 * Therefore this implementation is best suited to people who wish to use the mbox format
 * for taking data out of James and into something else (IMAP server or mail list displayer)
 *
 * @version CVS $Revision$
 * @deprecated
 */


public class MboxRepository {

	static final SimpleDateFormat dy = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy", Locale.US);
    static final String LOCKEXT = ".lock";
    static final String WORKEXT = ".work";
    static final int LOCKSLEEPDELAY = 2000; // 2 second back off in the event of a problem with the lock file
    static final int MAXSLEEPTIMES = 100; //
    static final long MLISTPRESIZEFACTOR = 10 * 1024;  // The hash table will be loaded with a initial capacity of  filelength/MLISTPRESIZEFACTOR
    static final long DEFAULTMLISTCAPACITY = 20; // Set up a hashtable to have a meaningful default
    static final Logger logger = new Logger(false);
    
    private int collisions = 0;
    /**
     * Whether line buffering is turned used.
     */
    private static boolean BUFFERING = true;

    /**
     * Whether 'deep debugging' is turned on.
     */
    private static final boolean DEEP_DEBUG = false;

    /**
     * The internal list of the emails
     * The key is an adapted MD5 checksum of the mail
     */
    @SuppressWarnings("unchecked")
	private Hashtable mList = null;
    /**
     * The filename to read & write the mbox from/to
     */
    private String mboxFile;

    /**
     * A callback used when a message is read from the mbox file
     */
    public interface MessageAction {
        public boolean isComplete();  // *** Not valid until AFTER each call to messageAction(...)!
        public MimeMessage messageAction(String messageSeparator, String bodyText, long messageStart);
    }

    public MboxRepository(String pathToFile) {

    	File f = new File(pathToFile);
    	if (! f.exists()) {
    		getLogger().error("Mailbox File " + pathToFile + " does not exist!");
    		this.mboxFile = null;
    	}

    	this.mboxFile = pathToFile;
    }

    /**
     * Convert a MimeMessage into raw text
     * @param mc The mime message to convert
     * @return A string representation of the mime message
     * @throws IOException
     * @throws MessagingException
     */
    public String getRawMessage(MimeMessage mc) throws IOException, MessagingException {

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mc.writeTo(rawMessage);
        return rawMessage.toString();
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
            System.err.println("Unable to parse mime message!" + "\n" + e.getMessage());
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

    /**
     * Generate a hex representation of an MD5 checksum on the emailbody
     * @param emailBody
     * @return A hex representation of the text
     * @throws NoSuchAlgorithmException
     */
    private String generateKeyValue(String emailBody) throws NoSuchAlgorithmException {
        // MD5 the email body for a reilable (ha ha) key
        byte[] digArray = MessageDigest.getInstance("MD5").digest(emailBody.getBytes());
        StringBuffer digest = new StringBuffer();
        for (int i = 0; i < digArray.length; i++) {
            digest.append(Integer.toString(digArray[i], Character.MAX_RADIX).toUpperCase(Locale.US));
        }
        return digest.toString();
    }

    /**
     * Parse the mbox file.
     * @param ins The random access file to load. Note that the file may or may not start at offset 0 in the file
     * @param messAct The action to take when a message is found
     */
    private MimeMessage parseMboxFile(RandomAccessFile ins, MessageAction messAct) {
        if ((DEEP_DEBUG)) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append(" Start parsing ")
                    .append(mboxFile);

            System.err.println(logBuffer.toString());
        }
        try {

            Perl5Compiler sepMatchCompiler = new Perl5Compiler();
            Pattern sepMatchPattern = sepMatchCompiler.compile("^From (.*) (.*):(.*):(.*)$");
            Pattern headerMatchPattern = sepMatchCompiler.compile("(.*):(.*)");
            Perl5Matcher sepMatch = new Perl5Matcher();
            Perl5Matcher headerMatch = new Perl5Matcher();

            int c;
            boolean inMessage = false;
            StringBuffer messageBuffer = new StringBuffer();
            String previousMessageSeparator = null;
            boolean foundSep = false;
            int headersFound = 0;
            
            long prevMessageStart = ins.getFilePointer();
            if (BUFFERING) {
            String line = null;
            while ((line = ins.readLine()) != null) {
                foundSep = sepMatch.contains(line + "\n", sepMatchPattern);
                if (headerMatch.contains(line + "\n", headerMatchPattern)) {
                	headersFound++;
                }
                if (foundSep && inMessage) {
                	if (headersFound >= 2) {
                		MimeMessage endResult = messAct.messageAction(previousMessageSeparator, messageBuffer.toString(), prevMessageStart);
                		if (messAct.isComplete()) {
                			// I've got what I want so just exit
                			return endResult;
                		}
                		previousMessageSeparator = line;
                		prevMessageStart = ins.getFilePointer() - line.length();
                		messageBuffer = new StringBuffer();
                		inMessage = true;
                		headersFound = 0;
                	}
                }
                // Only done at the start (first header)
                if (foundSep && !inMessage) {
                    previousMessageSeparator = line.toString();
                    inMessage = true;
                }
                if (!foundSep && inMessage) {
                    messageBuffer.append(line).append("\n");
                }
            }
            } else {
            StringBuffer line = new StringBuffer();
            while ((c = ins.read()) != -1) {
                if (c == 10) {
                    foundSep = sepMatch.contains(line.toString(), sepMatchPattern);
                    if (foundSep && inMessage) {
//                        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
//                            getLogger().debug(this.getClass().getName() + " Invoking " + messAct.getClass() + " at " + prevMessageStart);
//                        }
                        MimeMessage endResult = messAct.messageAction(previousMessageSeparator, messageBuffer.toString(), prevMessageStart);
                        if (messAct.isComplete()) {
                            // I've got what I want so just exit
                            return endResult;
                        }
                        previousMessageSeparator = line.toString();
                        prevMessageStart = ins.getFilePointer() - line.length();
                        messageBuffer = new StringBuffer();
                        inMessage = true;
                    }
                    // Only done at the start (first header)
                    if (foundSep && inMessage == false) {
                        previousMessageSeparator = line.toString();
                        inMessage = true;
                    }
                    if (!foundSep) {
                        messageBuffer.append(line).append((char) c);
                    }
                    line = new StringBuffer(); // Reset buffer
                } else {
                    line.append((char) c);
                }
            }
            }

            if (messageBuffer.length() != 0) {
                // process last message
                return messAct.messageAction(previousMessageSeparator, messageBuffer.toString(), prevMessageStart);
            }
        } catch (IOException ioEx) {
        	System.err.println("Unable to write file (General I/O problem) " + mboxFile + "\n" + ioEx.getMessage());
        } catch (MalformedPatternException e) {
        	System.err.println("Bad regex passed " + mboxFile + "\n" +  e.getMessage());
        } finally {
            if ((DEEP_DEBUG)) {
                StringBuffer logBuffer =
                        new StringBuffer(128)
                        .append(this.getClass().getName())
                        .append(" Finished parsing ")
                        .append(mboxFile);

                System.err.println(logBuffer.toString());
            }
        }
        return null;
    }

    /**
     * Find a given message
     * This method will first use selectMessage(key) to see if the key/offset combination allows us to skip
     * parts of the file and only load the message we are interested in
     *
     * @param key The key of the message to find
     */
    private MimeMessage findMessage(String key) {
        MimeMessage foundMessage = null;

        // See if we can get the message by using the cache position first
        foundMessage = selectMessage(key);
        if (foundMessage == null) {
            // If the message is not found something has changed from
            // the cache.  The cache may have been invalidated by
            // another method, or the file may have been replaced from
            // underneath us.  Reload the cache, and try again.
            mList = null;
            loadKeys();
            foundMessage = selectMessage(key);
        }
        return foundMessage;
    }

    /**
     * Quickly find a message by using the stored message offsets
     * @param key  The key of the message to find
     */
    private MimeMessage selectMessage(final String key) {
        MimeMessage foundMessage = null;
        // Can we find the key first
        if (mList == null || !mList.containsKey(key)) {
            // Not initiailised so no point looking
            if ((DEEP_DEBUG)) {
                StringBuffer logBuffer =
                        new StringBuffer(128)
                        .append(this.getClass().getName())
                        .append(" mList - key not found ")
                        .append(mboxFile);

                System.err.println(logBuffer.toString());
            }
            return foundMessage;
        }
        long messageStart = ((Long) mList.get(key)).longValue();
        if ((DEEP_DEBUG)) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append(" Load message starting at offset ")
                    .append(messageStart)
                    .append(" from file ")
                    .append(mboxFile);

            System.err.println(logBuffer.toString());
        }
        // Now try and find the position in the file
        RandomAccessFile ins = null;
        try {
            ins = new RandomAccessFile(mboxFile, "r");
            if (messageStart != 0) {
                ins.seek(messageStart - 1);
            }
            MessageAction op = new MessageAction() {
                public boolean isComplete() { return true; }
                public MimeMessage messageAction(String messageSeparator, String bodyText, long messageStart) {
                    try {
                        if (key.equals(generateKeyValue(bodyText))) {
                        	if (DEEP_DEBUG)
                        		System.err.println(this.getClass().getName() + "Success! Located message. Returning MIME message");
                            return convertTextToMimeMessage(bodyText);
                        }
                    } catch (NoSuchAlgorithmException e) {
                    	System.err.println("MD5 not supported! " + "\n" + e.getMessage());
                    }
                    return null;
                }
            };
            foundMessage = this.parseMboxFile(ins, op);
        } catch (FileNotFoundException e) {
        	System.err.println("Unable to save(open) file (File not found) " + mboxFile + "\n" + e.getMessage());
        } catch (IOException e) {
        	System.err.println("Unable to write file (General I/O problem) " + mboxFile  + "\n" + e.getMessage());
        } finally {
            if (foundMessage == null) {
                if ((DEEP_DEBUG)) {
                    StringBuffer logBuffer =
                            new StringBuffer(128)
                            .append(this.getClass().getName())
                            .append(" select - message not found ")
                            .append(mboxFile);

                    System.err.println(logBuffer.toString());
                }
            }
            if (ins != null) try { ins.close(); } catch (IOException e) { getLogger().error("Unable to close file (General I/O problem) " + mboxFile, e); }
        }
        return foundMessage;
    }

    private Logger getLogger() {
		return MboxRepository.logger;
	}

	/**
     * Load the message keys and file pointer offsets from disk
     */
    @SuppressWarnings("unchecked")
	private synchronized void loadKeys() {
        if (mList!=null) {
            return;
        }
        RandomAccessFile ins = null;
        try {
            ins = new RandomAccessFile(mboxFile, "r");
            long initialCapacity = (ins.length() >  MLISTPRESIZEFACTOR ? ins.length() /MLISTPRESIZEFACTOR  : 0);
            if (initialCapacity < DEFAULTMLISTCAPACITY ) {
                initialCapacity =  DEFAULTMLISTCAPACITY;
            }
            if (initialCapacity > Integer.MAX_VALUE) {
                initialCapacity = Integer.MAX_VALUE - 1;
            }
            this.mList = new Hashtable((int)initialCapacity);
            this.parseMboxFile(ins, new MessageAction() {
                public boolean isComplete() { return false; }
                public MimeMessage messageAction(String messageSeparator, String bodyText, long messageStart) {
                    try {
                        String key = generateKeyValue(bodyText);
                        if (mList.containsKey(key) ) {
                        	if (DEEP_DEBUG)
                        		getLogger().error("Attention! MessageTable already contains entry " + key + "\n Potential Collision!");
                        	collisions++;
                        } else {
                        	mList.put(key, new Long(messageStart));
                        	if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        		getLogger().debug(this.getClass().getName() + " Key " + key + " at " + messageStart);
                        	}
                        }
                        
                    } catch (NoSuchAlgorithmException e) {
                        getLogger().error("MD5 not supported! ",e);
                    }
                    return null;
                }
            });
            //System.out.println("Done Load keys!");
        } catch (FileNotFoundException e) {
            getLogger().error("Unable to save(open) file (File not found) " + mboxFile, e);
            this.mList = new Hashtable((int)DEFAULTMLISTCAPACITY);
        } catch (IOException e) {
            getLogger().error("Unable to write file (General I/O problem) " + mboxFile, e);
        } finally {
            if (ins != null) try { ins.close(); } catch (IOException e) { getLogger().error("Unable to close file (General I/O problem) " + mboxFile, e); }
        }
    }


    /**
     * Return the list of the current messages' keys
     * @return A list of the keys of the emails currently loaded
     */
    @SuppressWarnings("unchecked")
	public Iterator list() {
        loadKeys();
        
        if (mList.keySet().isEmpty() == false) {
            // find the first message.  This is a trick to make sure that if
            // the file is changed out from under us, we will detect it and
            // correct for it BEFORE we return the iterator.
            findMessage((String) mList.keySet().iterator().next());
        }
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append(" ")
                    .append(mList.size())
                    .append(" keys to be iterated over.");

            getLogger().debug(logBuffer.toString());
        }
        return mList.keySet().iterator();
    }

    /**
     * Get a message from the backing store (disk)
     * @param key
     * @return The mail found from the key. Returns null if the key is not found
     */
    public Message retrieve(String key) {
        loadKeys(); 
        MimeMessage foundMessage = null;

        foundMessage = findMessage(key);
        if (foundMessage == null) {
        	getLogger().error("found message is null!");
        	return null;
        }

        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
        	StringBuffer logBuffer =
        		new StringBuffer(128)
        	.append(this.getClass().getName())
        	.append(" Retrieving entry for key ")
        	.append(key);

        	getLogger().debug(logBuffer.toString());
        }

        return foundMessage;
    }

    /**
     * Attempt to get a lock on the mbox by creating
     * the file mboxname.lock
     * @throws Exception
     */
    /*private void lockMBox() throws Exception {
        // Create the lock file (if possible)
        String lockFileName = mboxFile + LOCKEXT;
        int sleepCount = 0;
        File mBoxLock = new File(lockFileName);
        if (!mBoxLock.createNewFile()) {
            // This is not good, somebody got the lock before me
            // So wait for a file
            while (!mBoxLock.createNewFile() && sleepCount < MAXSLEEPTIMES) {
                try {
                    if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        StringBuffer logBuffer =
                                new StringBuffer(128)
                                .append(this.getClass().getName())
                                .append(" Waiting for lock on file ")
                                .append(mboxFile);

                        getLogger().debug(logBuffer.toString());
                    }

                    Thread.sleep(LOCKSLEEPDELAY);
                    sleepCount++;
                } catch (InterruptedException e) {
                    getLogger().error("File lock wait for " + mboxFile + " interrupted!",e);

                }
            }
            if (sleepCount >= MAXSLEEPTIMES) {
                throw new Exception("Unable to get lock on file " + mboxFile);
            }
        }
    }*/

    
    /**
     * Unlock a previously locked mbox file
     * never used locally so far
     */
/*    private void unlockMBox() {
        // Just delete the MBOX file
        String lockFileName = mboxFile + LOCKEXT;
        File mBoxLock = new File(lockFileName);
        if (!mBoxLock.delete()) {
            StringBuffer logBuffer =
                    new StringBuffer(128)
                    .append(this.getClass().getName())
                    .append(" Failed to delete lock file ")
                    .append(lockFileName);
            getLogger().error(logBuffer.toString());
        }
    }
*/

    /**
     * Not implemented
     * @param key
     * @return
     */
    public boolean lock(String key) {
        return false;
    }

    /**
     * Not implemented
     * @param key
     * @return
     */
    public boolean unlock(String key) {
        return false;
    }
    
    public int getCollisions() {
    	return collisions;
    }
}