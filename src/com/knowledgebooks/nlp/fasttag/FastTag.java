// Copyright 2003-2008.  Mark Watson (markw@markwatson.com).  All rights reserved.
// This software is released under the LGPL (www.fsf.org)
// For an alternative non-GPL license: contact the author
// THIS SOFTWARE COMES WITH NO WARRANTY

package com.knowledgebooks.nlp.fasttag;

import java.io.*;
import java.util.*;

/**
 * <p/>
 * Copyright 2002-2007 by Mark Watson. All rights reserved.
 * <p/>
 */
public class FastTag {

	private static final Map<String, String[]> lexicon = buildLexicon();

	/**
	 * 
	 * @param word
	 * @return true if the input word is in the lexicon, otherwise return false
	 */
	public static boolean wordInLexicon(String word) {
		String[] ss = lexicon.get(word);
		if (ss != null)
			return true;
		// 1/22/2002 mod (from Lisp code): if not in hash, try lower case:
		if (ss == null)
			ss = lexicon.get(word.toLowerCase());
		if (ss != null)
			return true;
		return false;
	}

	/**
	 * 
	 * @param words
	 *            list of strings to tag with parts of speech
	 * @return list of strings for part of speech tokens
	 */
	public static List<String> tag(List<String> words) {
		List<String> ret = new ArrayList<String>(words.size());
		for (int i = 0, size = words.size(); i < size; i++) {
			String[] ss = (String[]) lexicon.get(words.get(i));
			// 1/22/2002 mod (from Lisp code): if not in hash, try lower case:
			if (ss == null)
				ss = lexicon.get(words.get(i).toLowerCase());
			if (ss == null && words.get(i).length() == 1)
				ret.add(words.get(i) + "^");
			else if (ss == null)
				ret.add("NN");
			else
				ret.add(ss[0]);
		}
		/**
		 * Apply transformational rules
		 **/
		for (int i = 0; i < words.size(); i++) {
			String word = ret.get(i);
			// rule 1: DT, {VBD | VBP} --> DT, NN
			if (i > 0 && ret.get(i - 1).equals("DT")) {
				if (word.equals("VBD") || word.equals("VBP") || word.equals("VB")) {
					ret.set(i, "NN");
				}
			}
			// rule 2: convert a noun to a number (CD) if "." appears in the word
			if (word.startsWith("N")) {
				if (words.get(i).indexOf(".") > -1) {
					ret.set(i, "CD");
				}
				try {
					Float.parseFloat(words.get(i));
					ret.set(i, "CD");
				} catch (Exception e) { // ignore: exception OK: this just means
										// that the string could not parse as a
										// number
				}
			}
			// rule 3: convert a noun to a past participle if words.get(i) ends with "ed"
			if (ret.get(i).startsWith("N") && words.get(i).endsWith("ed"))
				ret.set(i, "VBN");
			// rule 4: convert any type to adverb if it ends in "ly";
			if (words.get(i).endsWith("ly"))
				ret.set(i, "RB");
			// rule 5: convert a common noun (NN or NNS) to a adjective if it ends with "al"
			if (ret.get(i).startsWith("NN") && words.get(i).endsWith("al"))
				ret.set(i, "JJ");
			// rule 6: convert a noun to a verb if the preceeding work is "would"
			if (i > 0 && ret.get(i).startsWith("NN")
					&& words.get(i - 1).equalsIgnoreCase("would"))
				ret.set(i, "VB");
			// rule 7: if a word has been categorized as a common noun and it ends with "s",
			// then set its type to plural common noun (NNS)
			if (ret.get(i).equals("NN") && words.get(i).endsWith("s"))
				ret.set(i, "NNS");
			// rule 8: convert a common noun to a present participle verb (i.e., a gerand)
			if (ret.get(i).startsWith("NN") && words.get(i).endsWith("ing"))
				ret.set(i, "VBG");
		}
		return ret;
	}

	/**
	 * Simple main test program
	 * 
	 * @param args
	 *            string to tokenize and tag
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: argument is a string like \"The ball rolled down the street.\"\n\nSample run:\n");
			List<String> words = com.knowledgebooks.nlp.util.Tokenizer.wordsToList("The ball rolled down the street.");
			List<String> tags = tag(words);
			for (int i = 0; i < words.size(); i++)
				System.out.println(words.get(i) + "/" + tags.get(i));
		} else {
			List<String> words = com.knowledgebooks.nlp.util.Tokenizer.wordsToList(args[0]);
			List<String> tags = tag(words);
			for (int i = 0; i < words.size(); i++)
				System.out.println(words.get(i) + "/" + tags.get(i));
		}
	}

	private static Map<String, String[]> buildLexicon() {
		Map<String, String[]> lexicon = new HashMap<String, String[]>();
		try {
			InputStream ins = FastTag.class.getClassLoader().getResourceAsStream("lexicon.txt");
			if (ins == null) {
				ins = new FileInputStream("data/lexicon.txt");
			}
			Scanner scanner = new Scanner(ins);
			scanner.useDelimiter(System.getProperty("line.separator"));
			while (scanner.hasNext()) {
				String line = scanner.next();
				int count = 0;
				for (int i = 0, size = line.length(); i < size; i++) {
					if (line.charAt(i) == ' ') {
						count++;
					}
				}
				if (count == 0) {
					continue;
				}
				String[] ss = new String[count];
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(" ");
				String word = lineScanner.next();
				count = 0;
				while (lineScanner.hasNext()) {
					ss[count++] = lineScanner.next();
				}
				lineScanner.close();
				lexicon.put(word, ss);
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.unmodifiableMap(lexicon);
	}

}
