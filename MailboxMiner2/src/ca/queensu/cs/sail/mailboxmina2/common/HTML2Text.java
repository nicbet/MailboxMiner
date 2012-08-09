package ca.queensu.cs.sail.mailboxmina2.common;

import java.util.HashMap;

/**
 * The HTML2Text class can be used to transform HTML to plain text.
 * Source code was derived from APACHE James Mailserver.
 * @author Nicolas Bettenburg
 *
 */
public class HTML2Text {

	 @SuppressWarnings("unchecked")
	private final HashMap charMap = new HashMap();
	
	 /**
	  * Convert a {@link String} containing HTML to plain text
	  * @param html a {@link String} containing HTML text to be converted.
	  * @return a {@link String} containing the plain text (Unicode) representation of the input.
	  */
	public String html2Text(String html) {
        return decodeEntities(html
            .replaceAll("\\<([bB][rR]|[dD][lL])[ ]*[/]*[ ]*\\>", "\n")
            .replaceAll("\\</([pP]|[hH]5|[dD][tT]|[dD][dD]|[dD][iI][vV])[ ]*\\>", "\n")
            .replaceAll("\\<[lL][iI][ ]*[/]*[ ]*\\>", "\n* ")
            .replaceAll("\\<[dD][dD][ ]*[/]*[ ]*\\>", " - ")
            .replaceAll("\\<.*?\\>", ""));
    }
	
	/**
	 * Standard constructor.
	 */
	public HTML2Text() {
		initEntityTable();
	}
    
	/**
	 * Decode escaped entities as seen in HTML documents to their plain text correspondences.
	 * @param data a {@link String} containing the input to be decoded.
	 * @return a {@link String} containing the modified input.
	 */
    public String decodeEntities(String data) {
        StringBuffer buffer = new StringBuffer();
        StringBuffer res = new StringBuffer();
        int lastAmp = -1;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);

            if (c == '&' && lastAmp == -1) lastAmp = buffer.length();
            else if (c == ';' && (lastAmp > -1)) { // && (lastAmp > (buffer.length() - 7))) { // max: &#xxxx;
                if (charMap.containsKey(buffer.toString())) res.append((String) charMap.get(buffer.toString()));
                else res.append("&" + buffer.toString() + ";");
                lastAmp = -1;
                buffer = new StringBuffer();
            } 
            else if (lastAmp == -1) res.append(c);
            else buffer.append(c);
        }
        return res.toString();
    }

    /**
     * Initialization function. To be called by the constructor!
     */
    @SuppressWarnings("unchecked")
	private final void initEntityTable() {
        for (int index = 11; index < 32; index++) charMap.put("#0" + index, String.valueOf((char) index));
        for (int index = 32; index < 128; index++) charMap.put("#" + index, String.valueOf((char) index));
        for (int index = 128; index < 256; index++) charMap.put("#" + index, String.valueOf((char) index));
        
        // A complete reference is here:
        // http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
        
        charMap.put("#09", "\t");
        charMap.put("#10", "\n");
        charMap.put("#13", "\r");
        charMap.put("#60", "<");
        charMap.put("#62", ">");

        charMap.put("lt", "<");
        charMap.put("gt", ">");
        charMap.put("amp", "&");
        charMap.put("nbsp", " ");
        charMap.put("quot", "\"");

        charMap.put("iexcl", "\u00A1");
        charMap.put("cent", "\u00A2");
        charMap.put("pound", "\u00A3");
        charMap.put("curren", "\u00A4");
        charMap.put("yen", "\u00A5");
        charMap.put("brvbar", "\u00A6");
        charMap.put("sect", "\u00A7");
        charMap.put("uml", "\u00A8");
        charMap.put("copy", "\u00A9");
        charMap.put("ordf", "\u00AA");
        charMap.put("laquo", "\u00AB");
        charMap.put("not", "\u00AC");
        charMap.put("shy", "\u00AD");
        charMap.put("reg", "\u00AE");
        charMap.put("macr", "\u00AF");
        charMap.put("deg", "\u00B0");
        charMap.put("plusmn", "\u00B1");
        charMap.put("sup2", "\u00B2");
        charMap.put("sup3", "\u00B3");

        charMap.put("acute", "\u00B4");
        charMap.put("micro", "\u00B5");
        charMap.put("para", "\u00B6");
        charMap.put("middot", "\u00B7");
        charMap.put("cedil", "\u00B8");
        charMap.put("sup1", "\u00B9");
        charMap.put("ordm", "\u00BA");
        charMap.put("raquo", "\u00BB");
        charMap.put("frac14", "\u00BC");
        charMap.put("frac12", "\u00BD");
        charMap.put("frac34", "\u00BE");
        charMap.put("iquest", "\u00BF");

        charMap.put("Agrave", "\u00C0");
        charMap.put("Aacute", "\u00C1");
        charMap.put("Acirc", "\u00C2");
        charMap.put("Atilde", "\u00C3");
        charMap.put("Auml", "\u00C4");
        charMap.put("Aring", "\u00C5");
        charMap.put("AElig", "\u00C6");
        charMap.put("Ccedil", "\u00C7");
        charMap.put("Egrave", "\u00C8");
        charMap.put("Eacute", "\u00C9");
        charMap.put("Ecirc", "\u00CA");
        charMap.put("Euml", "\u00CB");
        charMap.put("Igrave", "\u00CC");
        charMap.put("Iacute", "\u00CD");
        charMap.put("Icirc", "\u00CE");
        charMap.put("Iuml", "\u00CF");

        charMap.put("ETH", "\u00D0");
        charMap.put("Ntilde", "\u00D1");
        charMap.put("Ograve", "\u00D2");
        charMap.put("Oacute", "\u00D3");
        charMap.put("Ocirc", "\u00D4");
        charMap.put("Otilde", "\u00D5");
        charMap.put("Ouml", "\u00D6");
        charMap.put("times", "\u00D7");
        charMap.put("Oslash", "\u00D8");
        charMap.put("Ugrave", "\u00D9");
        charMap.put("Uacute", "\u00DA");
        charMap.put("Ucirc", "\u00DB");
        charMap.put("Uuml", "\u00DC");
        charMap.put("Yacute", "\u00DD");
        charMap.put("THORN", "\u00DE");
        charMap.put("szlig", "\u00DF");

        charMap.put("agrave", "\u00E0");
        charMap.put("aacute", "\u00E1");
        charMap.put("acirc", "\u00E2");
        charMap.put("atilde", "\u00E3");
        charMap.put("auml", "\u00E4");
        charMap.put("aring", "\u00E5");
        charMap.put("aelig", "\u00E6");
        charMap.put("ccedil", "\u00E7");
        charMap.put("egrave", "\u00E8");
        charMap.put("eacute", "\u00E9");
        charMap.put("ecirc", "\u00EA");
        charMap.put("euml", "\u00EB");
        charMap.put("igrave", "\u00EC");
        charMap.put("iacute", "\u00ED");
        charMap.put("icirc", "\u00EE");
        charMap.put("iuml", "\u00EF");

        charMap.put("eth", "\u00F0");
        charMap.put("ntilde", "\u00F1");
        charMap.put("ograve", "\u00F2");
        charMap.put("oacute", "\u00F3");
        charMap.put("ocirc", "\u00F4");
        charMap.put("otilde", "\u00F5");
        charMap.put("ouml", "\u00F6");
        charMap.put("divid", "\u00F7");
        charMap.put("oslash", "\u00F8");
        charMap.put("ugrave", "\u00F9");
        charMap.put("uacute", "\u00FA");
        charMap.put("ucirc", "\u00FB");
        charMap.put("uuml", "\u00FC");
        charMap.put("yacute", "\u00FD");
        charMap.put("thorn", "\u00FE");
        charMap.put("yuml", "\u00FF");
        charMap.put("euro", "\u0080");
    }
	
}
