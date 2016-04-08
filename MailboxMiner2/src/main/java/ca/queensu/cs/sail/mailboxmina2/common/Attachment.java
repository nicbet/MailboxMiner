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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ca.queensu.cs.sail.mailboxmina2.main.Main;

/**
 * The Attachment class represents a (virtual) file extracted from a {@link Message}.
 * @author Nicolas Bettenburg
 *
 */
/**
 * @author nicbet
 *
 */
public class Attachment {

	private String mime_type;
	private String original_filename;
	private byte[] data;
	
	/**
	 * Overloaded constructor.
	 * @param data a byte array containing the data for the file.
	 * @param mime_type a description of the file's mime-type.
	 * @param original_filename the original file's name if known.
	 */
	public Attachment(byte[] data, String mime_type, String original_filename) {
		super();
		this.data = data;
		this.mime_type = mime_type;
		this.original_filename = original_filename;
	}

	/**
	 * Standard constructor
	 */
	public Attachment() {
		super();
	}

	/**
	 * Getter method
	 * @return a {@link String} containing the file's mime-type
	 */
	public String getMime_type() {
		return mime_type;
	}

	
	/**
	 * Setter method
	 * @param mime_type a {@link String} containing the file's mime-type.
	 */
	public void setMime_type(String mime_type) {
		this.mime_type = mime_type;
	}

	/**
	 * Getter method
	 * @return a {@link String} containing the file's original filename.
	 */
	public String getOriginal_filename() {
		return original_filename;
	}

	/**
	 * Setter method
	 * @param original_filename a {@link String} containing the file's original filename.
	 */
	public void setOriginal_filename(String original_filename) {
		this.original_filename = original_filename;
	}
	
	/**
	 * Getter method
	 * @return a byte array containing the data of the file.
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * Setter method 
	 * @param data a byte array representing the file's data.
	 */
	public void setData(byte[] data) {
		this.data = data;
	}
	
	/**
	 * Compress the data contained in the attachment in the gzip format.
	 * @return a byte array containing the gzip compressed data.
	 */
	public byte[] getZippedData () {
		try {
			ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
			java.util.zip.GZIPOutputStream zipStream = new java.util.zip.GZIPOutputStream(ba_out);
			zipStream.write(data, 0, data.length);
			zipStream.finish();
			zipStream.close();
			return ba_out.toByteArray();
		} catch (IOException e) {
			Main.getLogger().error("Unable to compress the attachment data!", e);
		}
		return new byte[0];
	}
	
}
