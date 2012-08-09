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
package ca.queensu.cs.sail.mailboxmina2.test;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;

public class ListCharsets {
  static public void main(String args[]) throws Exception {
    SortedMap charsets = Charset.availableCharsets();
    Set names = charsets.keySet();
    for (Iterator e = names.iterator(); e.hasNext();) {
      String name = (String) e.next();
      Charset charset = (Charset) charsets.get(name);
      System.out.println(charset);
      Set aliases = charset.aliases();
      for (Iterator ee = aliases.iterator(); ee.hasNext();) {
        System.out.println("    " + ee.next());
      }
    }
  }
}
