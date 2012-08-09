package ca.queensu.cs.sail.mailboxmina2.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.*;




public class SepLineTest {


		public static void main(String[] args) {
			String file = args[0];
			parseMessages(file);
		}

		public static void parseMessages(String filename) {
			// Open the file for reading
			try {
				StringBuilder inputBuilder = new StringBuilder();
				String line = "";
				BufferedReader reader = new BufferedReader(new FileReader(filename));
				// Read the mbox file line by line
				while ((line = reader.readLine()) != null) {
					inputBuilder.append(line);
					inputBuilder.append(System.getProperty("line.separator"));
				}

				String text = inputBuilder.toString();
				inputBuilder = null;

				String[] rawlines = text.split("(\n\r)|(\n)|(\r)");

				//Pattern seperatorPattern = Pattern.compile("^From (.*?) (.*?):(.*?):(.*?)$");
				Pattern seperatorPattern = Pattern.compile("^From ([^ ]*?)\\s*?[\\x00-\\x7F]{24}.*$");
				Pattern headerPattern = Pattern.compile("^[\\x21-\\x39\\x3B-\\x7E]+:(.*)$");		// From RFC 5322 - Oct 2008
				String ssep = System.getProperty("line.separator");

				// Here comes the big ugly loop ...
			
				for (int i=0; i < rawlines.length; i++) {
					String theline = rawlines[i];
					if (seperatorPattern.matcher(theline).matches())
						System.out.println("[" + i + "] " + theline);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}

	}

