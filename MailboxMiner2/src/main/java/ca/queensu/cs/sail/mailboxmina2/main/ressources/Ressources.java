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
package ca.queensu.cs.sail.mailboxmina2.main.ressources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import ca.queensu.cs.sail.mailboxmina2.main.Main;


/**
 * Helper class to read and mark ressources stored in the JAR archive.
 * @author Nicolas Bettenburg
 *
 */
public class Ressources {
	
	/**
	 * Get the contents of a specified ressource within this package.
	 * @param name a {@link String} containing the name of the ressource.
	 * @return a {@link String} containing the contents of the ressource.
	 */
	public String getRessourceContents(String name) {
		String contents = "";
		
		URL u = Ressources.class.getResource(name);
		try {
			BufferedReader r = new BufferedReader(new FileReader(new File(u.getFile())));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				sb.append(line + System.getProperty("line.separator"));
			}
			r.close();
			contents = sb.toString();
			
		} catch (FileNotFoundException e) {
			Main.getLogger().error(this, "Ressource not found " + name, e);
		} catch (IOException e) {
			Main.getLogger().error(this, "Error opening ressource " + name, e);
		}
		
		return contents;
	}
}
