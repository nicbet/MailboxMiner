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
package ca.queensu.cs.sail.mailboxmina2.common;

import java.io.PrintStream;

/**
 * This class provides basic logging facilities without
 * the need for a full blown framework like LOG4J ;-)
 * @author Nicolas Bettenburg
 *
 */
public class Logger {
	private boolean debugEnabled;
	private int maxDebugLevel= 1;
	private PrintStream logStream;
	private PrintStream debugStream;
	private PrintStream errorStream;
	
	public PrintStream getLogStream() {
		return logStream;
	}

	public void setLogStream(PrintStream logStream) {
		this.logStream = logStream;
	}

	public PrintStream getDebugStream() {
		return debugStream;
	}

	public void setDebugStream(PrintStream debugStream) {
		this.debugStream = debugStream;
	}

	public PrintStream getErrorStream() {
		return errorStream;
	}

	public void setErrorStream(PrintStream errorStream) {
		this.errorStream = errorStream;
	}

	public void setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
	}
	
	public Logger(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
		this.logStream = System.out;
		this.debugStream = System.out;
		this.errorStream = System.err;
	}
	
	public Logger() {
		this.debugEnabled = true;
		this.logStream = System.out;
		this.debugStream = System.out;
		this.errorStream = System.err;
	}
	
	public void error(String message, Exception e) {
		errorStream.println("[ERROR] " + message + "\n" + e.getMessage());
		e.printStackTrace(errorStream);
	}
	
	public void error(String message){
		errorStream.println("[ERROR] " + message);
	}
	
	public void error(Object c, String message, Exception e) {
		errorStream.println("[ERROR] " + c.getClass().getSimpleName() + " :: " + message + "\n" + e.getMessage());
		e.printStackTrace(errorStream);
	}
	
	public void error(Object c, String message){
		errorStream.println("[ERROR] " + c.getClass().getSimpleName() + " :: " + message);
	}
	
	
	public void warning(String message, Exception e) {
		errorStream.println("[WARNING] " + message + "\n" + e.getMessage());
		e.printStackTrace(errorStream);
	}
	
	public void warning(String message){
		errorStream.println("[WARNING] " + message);
	}
	
	public void warning(Object c, String message, Exception e) {
		errorStream.println("[WARNING] " + c.getClass().getSimpleName() + " :: " + message + "\n" + e.getMessage());
		e.printStackTrace(errorStream);
	}
	
	public void warning(Object c, String message){
		errorStream.println("[WARNING] " + c.getClass().getSimpleName() + " :: " + message);
	}
	
	
	public void debug(String message) {
		if (debugEnabled)
			debugStream.println("[DEBUG] " + message);
	}
	
	public void debug(Object c, String message) {
		if (debugEnabled)
			debugStream.println("[DEBUG] " + c.getClass().getSimpleName() + " :: " + message);
	}
	
	public void debug(int level, String message) {
		if (debugEnabled && level <= maxDebugLevel)
			debugStream.println("[DEBUG-" + level + "] " + message);
	}
	
	public void debug(int level, Object c, String message) {
		if (debugEnabled && level <= maxDebugLevel)
			debugStream.println("[DEBUG-" + level + "] " + c.getClass().getSimpleName() + " :: " + message);
	}
	
	public void log(String message) {
		logStream.println("[LOG] " + message);
	}
	
	public void log(Object c, String message) {
		logStream.println("[LOG] " + c.getClass().getSimpleName() + " :: " + message);
	}
	
	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void setMaxDebugLevel(int maxDebugLevel) {
		this.maxDebugLevel = maxDebugLevel;
	}

	public int getMaxDebugLevel() {
		return maxDebugLevel;
	}
	
	public void closeStreams() {
		logStream.close();
		debugStream.close();
		errorStream.close();
	}

	public void putSeparator(String where) {
		if (where.equalsIgnoreCase("debug"))
			debugStream.println("--------------------------------------------------------------------------------");
		else if (where.equalsIgnoreCase("error"))
			errorStream.println("--------------------------------------------------------------------------------");
		else if (where.equalsIgnoreCase("log"))
			logStream.println("--------------------------------------------------------------------------------");
	}
}
