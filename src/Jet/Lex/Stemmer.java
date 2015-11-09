// -*- tab-width: 4 -*-
package Jet.Lex;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import Jet.Lisp.FeatureSet;

import Jet.Console;
import Jet.Tipster.*;
import Jet.Util.IOUtils;

/**
 * Stemmer provides method for getting stem of word.
 * <p/>
 * Stemmer uses stem dictionary which is written in plain text. Each line of stem
 * dictionary will be as follows
 * <p/>
 * <pre>
 *  do	did does doing done
 * </pre>
 * <p/>
 * Each word is separated by whitepsace characters. First word is stem and other
 * words are inflected forms.
 *
 * @author Akira ODA
 * @author (revised 2015 R. Grishman)
 */

public class Stemmer {
    private static final String DICT_ENCODING = "US-ASCII";

    private static Stemmer defaultStemmer = null;

    private HashMap<String, String> dict = new HashMap<String, String>();

    public Stemmer() {
    }

    /**
     * Returns default stemmer.
     *
     * @return
     */
    public static Stemmer getDefaultStemmer() {
        if (defaultStemmer == null) {
            defaultStemmer = loadDefaultStemmer();
        }
        return defaultStemmer;
    }

    /**
     * Loads default stem dictionary.
     *
     * @return
     */
    private static Stemmer loadDefaultStemmer() {
        InputStream in = null;
        try {
            in = Stemmer.class.getClassLoader().getResourceAsStream(
																	"Jet/Lex/resources/stem.dict");
            Reader reader = new InputStreamReader(in, DICT_ENCODING);
            Stemmer stemmer = new Stemmer();
            stemmer.loadDictionary(reader);

            return stemmer;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }


    /**
     * Loads stem dictonary.
     *
     * @param file
     * @throws IOException
     */
    public void loadDictionary(File file) throws IOException {
        BufferedReader in = IOUtils.getBufferedReader(file, DICT_ENCODING);
        try {
            loadDictionary(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void loadDictionary(Reader reader) throws IOException {
        BufferedReader in = null;
        if (reader instanceof BufferedReader) {
            in = (BufferedReader) reader;
        } else {
            in = new BufferedReader(reader);
        }

        String line;
        Pattern delimiter = Pattern.compile("\\s+");
        while ((line = in.readLine()) != null) {
            String[] splitted = delimiter.split(line);
            String stem = splitted[0].intern();
            for (int i = 1; i < splitted.length; i++) {
                dict.put(splitted[i].intern(), stem);
            }
        }
    }

    /**
     * Added stem feature to each token annotation if token text and stem are
     * difference.
     *
     * @param doc
     * @param span
     */

    public void tagStem(Document doc, Span span) {
        Vector<Annotation> tokens = doc.annotationsOfType("token", span);
        Vector<String> posTags = getPosTags(doc, tokens);

        assert tokens.size() == posTags.size();

        for (int i = 0; i < tokens.size(); i++) {
            Annotation token = tokens.get(i);
            String word = doc.text(token).trim();
            String pos = posTags.get(i);
            String stem = getStem(word, pos);
            if (stem != word) {
                token.put("stem", stem);
            }
        }
    }

    /**
     * Returns stem of <code>word</code>
     *
     * @param word
     * @param pos  part of speech of <code>word</code>
     * @return stem of <code>word</code>.
     */

    public String getStem(String word, String pos) {
        if (word.equals("I") || pos.equals("NNP") || pos.equals("NNPS")) {
            return word;
        }

        String lower = word.toLowerCase();
        boolean allLower = lower.equals(word);
        String stem;
        if ((stem = dict.get(lower)) != null) {
            // known word
            return stem;
        }

        if (any(lower, "NNS", "VBZ")) {
            return getStemInternal(word, lower, "s", allLower);
        }

        if (any(lower, "VBD", "VBN")) {
            return getStemInternal(word, lower, "ed", allLower);
        }

        if (lower.equals("VBG")) {
            return getStemInternal(word, lower, "ing", allLower);
        }

        if (!allLower) {
            return lower;
        }

        return word;
    }

    private String getStemInternal(String word, String lowerWord, String suffix, boolean allLower) {
        if (lowerWord.endsWith(suffix)) {
            return lowerWord.substring(0, lowerWord.length() - suffix.length());
        } else if (!allLower) {
            return lowerWord;
        } else {
            return word;
        }
    }

    private Vector<String> getPosTags(Document doc, Vector<Annotation> tokens) {
        Vector<String> result = new Vector<String>();

        for (Annotation token : tokens) {
            Vector<Annotation> constitList = doc.annotationsOfType("constit", token.span());
            result.add(getPosTag(constitList));
        }

        return result;
    }

    private String getPosTag(Vector<Annotation> constitList) {
        if (constitList == null || constitList.size() == 0) {
            return null;
        } else if (constitList.size() == 1) {
            return ((String) constitList.get(0).get("cat")).toUpperCase();
        } else {
            for (Annotation constit : constitList) {
                Annotation[] children = (Annotation[]) constit.get("children");
                if (children != null) {
                    continue;
                }
                String cat = (String) constit.get("cat");
                if (cat != null) {
                    return cat.toUpperCase();
                }
            }
            return null;
        }
    }

    private static boolean any(String pos, String... candidates) {
        for (String candidate : candidates) {
            if (pos.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

	/**
	 *  converts a Jet English dictionary into the dictionary format used by the Stemmer.
	 *  Takes two arguments:  a file containing a Jet dictionary (input), and the
	 *  file into which the stem dictionary should be written.
	 */

	public static void main (String[] args) throws IOException {

		if (args.length != 2) {
			System.err.println("Stemmer requires two arguments: jetDictionary stemDictionary");
			System.exit(1);
		}
		String jetDict = args[0];
		String stemDict = args[1];
	
		EnglishLex.readLexicon(jetDict);
	
		Map<String, Set<String>> inflections = new TreeMap<String, Set<String>>();
		for (Object key : Lexicon.lexiconIndex.keySet()) {
			Vector<LexicalEntry> ev = (Vector<LexicalEntry>) Lexicon.lexiconIndex.get(key);
			for (LexicalEntry e : ev) {
				if (e.words.length == 1) {
					for (FeatureSet f : e.getDefinition()) {
						addInflection (inflections, e.words[0], f);
					}
				}
			}
		}
	
		PrintWriter writer = new PrintWriter (new FileWriter (stemDict));
		for (String lemma : inflections.keySet()) {
			writer.print(lemma);
			for (String inflectedForm : inflections.get(lemma))
				writer.print("\t" + inflectedForm);
			writer.println();
		}
		writer.close();
	}

	/**
	 *  if the 'features' for word 'word' include information on its
	 *  base form, and the base form differs from the inflected form,
	 *  add the inflected form to the 'inflections' map.
	 */
	
	private static void addInflection (Map<String, Set<String>> inflections, 
					   String word, FeatureSet features) {
		if (!(features.get("pa") instanceof FeatureSet)) return;
		FeatureSet pa = (FeatureSet) features.get("pa");
		if (pa == null) return;
		String lemma = (String) pa.get("head");
		if (lemma == null) return;
		if (word.equals(lemma)) return;
		if (inflections.get(lemma) == null)
			inflections.put(lemma, new HashSet<String>());
		inflections.get(lemma).add(word);
	}
}
