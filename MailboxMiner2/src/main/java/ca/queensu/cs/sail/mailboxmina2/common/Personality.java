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

public class Personality {
	private int seenCount = 0;
	private String p_name;
	private String p_address;
	
	public Personality(String name, String addr, int seen) {
		seenCount = seen;
		p_name = name;
		p_address = addr;
	}
	
	public boolean unknown_Name() {
		return (p_name.equalsIgnoreCase("Unknown Name"));
	}
	
	public boolean unknown_Address() {
		return (p_address.equalsIgnoreCase("unknown@address.com"));
	}
	
	public String getName(boolean normalized) {
		if (normalized) {
			return p_name;
		} else {
			String temp = p_name;
			temp = temp.replaceAll("[+_-\\.$%#!<>]", "");
			return (temp.toUpperCase());
		}
	}
	
	public String getAddress(boolean normalized) {
		if (normalized) {
			return p_address;
		} else {
			String temp = p_address;
			temp = temp.replaceAll("[ +_-\\.$%#!<>]", "");
			return (temp.toUpperCase());
		}
	}
	
	public int getSeen() {
		return seenCount;
	}
	
	public void seen() {
		seenCount++;
	}
	
	@Override
	public int hashCode() {
		return (getAddress(true).hashCode());
	}
	
}
