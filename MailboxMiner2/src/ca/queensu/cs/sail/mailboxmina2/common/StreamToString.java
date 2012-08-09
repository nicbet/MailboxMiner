package ca.queensu.cs.sail.mailboxmina2.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
 
/**
 * Helper class to convert an input stream to a Unicode string
 * in a brute force approach (ignoring character encoding and errors) 
 * @author Nicolas Bettenburg
 *
 */
public class StreamToString {
    
	/**
	 * Convert an {@link InputStream} to a String.
	 * @param is an {@link InputStream} to be read from.
	 * @return a {@link String} representing the contents of the {@link InputStream}.
	 * @throws IOException in case there are InputStream related errors.
	 */
    public static String convertStreamToString(InputStream is) throws IOException {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;

            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            
        is.close();
        return sb.toString();
    }
}