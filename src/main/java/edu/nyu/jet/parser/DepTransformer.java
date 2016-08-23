package edu.nyu.jet.parser;

import java.util.*;
import java.io.*;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.Control;
import edu.nyu.jet.tipster.*;
import edu.nyu.jet.lex.Stemmer;

/**
 *  a series of regularizing transformations for a dependency parser (currently the Tratz-Hovy parser).
 */

public class DepTransformer {

    SyntacticRelationSet relations;
    String transformList;
    boolean trace = false;
    Stemmer stemmer = Stemmer.getDefaultStemmer();
    boolean usePrepositionTransformation = false;
    // boolean useNegationTransformation = true;


    public void setUsePrepositionTransformation(boolean usePrepositionTransformation) {
        this.usePrepositionTransformation = usePrepositionTransformation;
    }

    //  public void setUseNegationTransformation(boolean useNegationTransformation) {
    //      this.useNegationTransformation = useNegationTransformation;
    //  }

    /**
     *  lists taken from Treebank 2a Guidelines,
     *  http://www-users.york.ac.uk/~lang22/TB2a_Guidelines.htm
     */

        Set<String> raisingVerbs =
            new HashSet<String> (Arrays.asList (new String[] {
	"appear", "begin", "continue", "end up", "fail", "figure",
	"happen", "keep", "need", "ought", "prove", "quit",
	"remain", "say", "seem", "start", "stop", "tend", "wind up"}));

    /*
     *  an attempt to distinguish negative raising verbs,
     *  currently disabled
     *
     *	Set<String> raisingVerbs =
     *       new HashSet<String> (Arrays.asList (new String[] {
     *      "begin", "continue", "keep", "prove", "remain", "start"}));
     *
     *	Set<String> negRaisingVerbs =
     *      new HashSet<String> (Arrays.asList (new String[] {
     *  	"fail", "quit", "stop"}));
     */

    Set<String> subjectControlVerbs = 
    new HashSet<String> (Arrays.asList (new String[] {
	"admit", "afford", "agree", "aim", "apply", "arrange",
	"ask", "attempt", "avoid", "be willing", "bother", "come",
	"care", "choose", "claim", "clamor", "concede", "conspire",
	"decide", "decline", "delay", "deny", "deserve", "determine",
	"disclaim", "discuss", "enjoy", "elect", "favor", "figure",
	"flock", "force", "forget", "get", "go to show", "hate",
	"hesitate", "hope", "intend", "jump", "know", "learn",
	"like", "look", "love", "manage", "mean", "mind",
	"miss", "move", "negotiate", "offer", "opt", "plan",
	"pledge", "plot", "pose", "ponder", "prefer", "prepare",
	"press", "proceed", "profess", "promise", "propose", "push",
	"quite", "race", "recall", "refuse", "report", "resolve",
	"risk", "rule out", "rush", "scramble", "seek", "serve",
	"set out", "sign", "sound", "stand", "strive", "struggle",
	"suffice", "swear", "threaten", "try", "undertake", "vote",
	"vow", "wait", "want", "wish"}));

    public DepTransformer (String transformList) {
	this.transformList = transformList;
	trace = transformList != null && transformList.contains("trace");
    }

    Map<Integer, String> wordMap = new HashMap<Integer, String>();
    Map<Integer, String> posMap = new HashMap<Integer, String>();

    /**
     *  perform a series of simplifying transformations on a dependency tree with 'relations',
     *  returning the simplified set of relations.
     */

    public SyntacticRelationSet transform (SyntacticRelationSet parseRelations, Span span) {

	if (transformList == null) return parseRelations;

	relations = parseRelations.deepCopy();

	// display parse
	if (trace) System.out.println(relations.toString());

        wordMap.clear();
        posMap.clear();
	for (int j=0; j<relations.size(); j++) {
	    SyntacticRelation r = (SyntacticRelation) relations.get(j);
	    wordMap.put(r.sourcePosn, r.sourceWord);
	    wordMap.put(r.targetPosn, r.targetWord);
	    posMap.put(r.sourcePosn, r.sourcePos);
	    posMap.put(r.targetPosn, r.targetPos);
	}

        //
        // concatenate particle to verb [look up --> look_up]
        //

        Set<Integer> internalNodes = findInternalNodes(relations, span);
        boolean modified = false;
        for (Integer i : internalNodes) {
            if(!isVerb(i)) continue;
            Integer prt = getNode(i, "prt");
            if (prt == null) continue;
            modified = true;
            relabelNode(i, getWord(i) + "_" + getWord(prt));
            removeEdge(i, prt);
        }

	//
	// fill in missing subject of adverbial clause from
	// matrix subject
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	for (Integer i : internalNodes) {
		Integer innerV = getNode(i, "advcl");
		if (innerV == null) continue;
		Integer outerSubj = getNode(i, "nsubj");
		Integer innerSubj = getNode(innerV, "nsubj");
		if (outerSubj == null || innerSubj != null) continue;
		modified = true;
		addEdge(innerV, "nsubj", outerSubj);
		relabel(i, innerV, "mod");
		transformTrace("adverbial clause");
	}
	
	//
	// expand reduced relatives with VING ("the man reading the book")
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
		Integer innerV = getNode(i, "partmod");
		if (innerV == null) continue;
		modified = true;
		addEdge(innerV, "nsubj", i);
		relabel(i, innerV, "mod");
		transformTrace("reduced relative (ving)");
	}

	//
	// expand reduced relatives with VEN ("the book read by the man")
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
		Integer innerV = getNode(i, "partmod");
		if (innerV == null) continue;
		modified = true;
		addEdge(innerV, "dobj", i);
		relabel(i, innerV, "mod");
		transformTrace("reduced relative (ven)");
	}

	//
	// fill in omitted arg of rel clause
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
		Integer innerV = getNode(i, "rcmod");
		if (innerV == null) continue;
		Integer relpro = getNode(innerV, "nsubj");
		if (relpro == null) continue;
		modified = true;
		removeEdge(innerV, relpro);
		addEdge(innerV, "nsubj", i);
		relabel(i, innerV, "mod");
		transformTrace("relative clause");
	}

	//
	// eliminate modals and perfect and progressive tense markers
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
	    if (!getPOS(i).equals("MD")) continue;
	    Integer v = getNode(i, "vch");
	    if (v == null) continue;
	    if (!getPOS(v).equals("VB")) continue;
	    modified = true;
	    String vWord = getWord(v);
	    removeEdge(i, v);
	    replaceAll (i, v);
	    transformTrace("modal");
	}

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
	    if (!isHave(getWord(i))) continue;
	    Integer v = getNode(i, "vch");
	    if (v == null) continue;
	    if (!getPOS(v).equals("VBN")) continue;
	    modified = true;
	    String vWord = getWord(v);
	    removeEdge(i, v);
	    replaceAll (i, v);
	    transformTrace("perfect tense");
	}

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
	    if (!isBe(getWord(i))) continue;
	    Integer v = getNode(i, "vch");
	    if (v == null) continue;
	    if (!getPOS(v).equals("VBG")) continue;
	    modified = true;
	    String vWord = getWord(v);
	    removeEdge(i, v);
	    replaceAll (i, v);
	    transformTrace("progressive");
	}

	//
	// convert passive clause to active form
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
	    if (!isBe(getWord(i))) continue;
	    Integer v = getNode(i, "vch");
	    if (v == null) continue;
	    if (!getPOS(v).equals("VBN")) continue;
	    modified = true;
	    if (trace) System.out.println("Found passive");
	    String vWord = getWord(v);
	    removeEdge(i, v);
	    replaceAll (i, v);
	    // change nsubj to dobj
	    Integer surfaceSubj = getNode(v, "nsubj");
	    if (surfaceSubj != null)
		relabel(v, surfaceSubj, "dobj");
	    // change agent--by to nsubj 
	    Integer agent = getNode(v, "agent");
	    if (agent == null) continue;
	    Integer by = getNode(agent, "pobj");
	    if (by == null) continue;
	    removeEdge(v, agent);
	    replaceAll(agent, v);
	    relabel(v, by, "nsubj");
	}

	//
	// for subject control, make matrix subject
	// also subject of embedded clause
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
	    if (!isVerb(i)) continue;
	    if (!subjectControlVerbs.contains(getLemma(i))) continue;
	    Integer v = getNode(i, "xcomp");
	    if (v == null) continue;
	    Integer nsubj = getNode(i, "nsubj");
	    if (nsubj == null) continue;
	    modified = true;
	    addEdge (v, "nsubj", nsubj);
	    transformTrace("subject control");
	}

	//
	// for raising verbs, move  matrix subject
	// to be  subject of embedded clause
	//

	if (modified) internalNodes = findInternalNodes(relations, span);
	modified = false;
	for (Integer i : internalNodes) {
	    if (!isVerb(i)) continue;
	    if (!raisingVerbs.contains(getLemma(i))) continue;
	    Integer v = getNode(i, "xcomp");
	    if (v == null) continue;
	    Integer nsubj = getNode(i, "nsubj");
	    if (nsubj == null) continue;
	    modified = true;
	    removeEdge (i, nsubj);
	    addEdge (v, "nsubj", nsubj);
	    transformTrace("raising");
	}

        //
        // for (neg) raising verbs, move  matrix subject
        // to be  subject of embedded clause
        //

        /*  currently disabled

		if (useNegationTransformation) {
			if (modified) internalNodes = findInternalNodes(relations, span);
			modified = false;
			for (Integer i : internalNodes) {
				if (!isVerb(i)) continue;
				if (!negRaisingVerbs.contains(getLemma(i))) continue;
				Integer v = getNode(i, "xcomp");
				if (v == null) continue;
				Integer nsubj = getNode(i, "nsubj");
				if (nsubj == null) continue;
				modified = true;
				removeEdge(i, nsubj);
				addEdge(v, "nsubj", nsubj);
				negate(v, relations);
				neutralize(i, relations);
				transformTrace("neg_raising");
			}
		}
        */

	//
	// convert preposition to arc label
	//
	if (usePrepositionTransformation) {
		if (modified) internalNodes = findInternalNodes(relations, span);
		modified = false;
		for (Integer i : internalNodes) {
			List<Integer> preps = getNodes(i, "prep");
			if (preps == null) continue;
			for (Integer prep : preps) {
				Integer pobj = getNode(prep, "pobj");
				if (pobj == null) continue;
				modified = true;
				relabel(i, prep, "prep_" + getWord(prep));
				removeEdge(prep, pobj);
				replaceAll(prep, pobj);
			}
		}
	}
	    
	// remove logically deleted relations

	SyntacticRelationSet newRelations = new SyntacticRelationSet();
	for (int j=0; j<relations.size(); j++) {
	    SyntacticRelation r = (SyntacticRelation) relations.get(j);
	    if (r.type != "")
		newRelations.add(r);
	}

	// display result of transformations
	if (trace) {
	    System.out.println("----Final dependency structure:");
	    System.out.println(newRelations.toString());
	    System.out.println("--------");
	}

	return newRelations;
    }

    String getWord (Integer i) {
	return wordMap.get(i);
    }

    String getPOS (Integer i) {
	return posMap.get(i);
    }

    String getLemma (Integer i) {
	return stemmer.getStem(getWord(i), getPOS(i));
    }

    static List<String> verbPos = Arrays.asList("VB", "VBZ", "VBD", "VBN", "VING");

    boolean isVerb(Integer i) {
	return verbPos.contains(getPOS(i));
    }
    
    /**
     *  returns a list of the target nodes reached from source node
     *  <CODE>i</CODE> by following arcs of type <CODE>type</CODE>,
     *  or <CODE>null</CODE> if no such arc exists..
     */
	
    List<Integer> getNodes (Integer i, String type) {
	List<Integer> result = null;
	SyntacticRelationSet srs = relations.getRelations(i, type);
	for (SyntacticRelation r : srs.relations) {
	    if (result == null) result = new ArrayList<Integer>();
	    result.add(r.targetPosn);
	}
	return result;
    }

    /**
     *  returns the target node reached from source node
     *  <CODE>i</CODE> by following the first arc of type <CODE>type</CODE>,
     *  or <CODE>null</CODE> if no such arc exists.
     */
	
    Integer getNode (Integer i, String type) {
	SyntacticRelation r = relations.getRelation(i, type);
	return (r == null) ? null : r.targetPosn;
    }

    void removeEdge (Integer from, Integer to) {
	SyntacticRelation r = relations.getRelation(from, to);
	r.type = "";
    }

    void addEdge (Integer from, String label, Integer to) {
        SyntacticRelation newRelation = new SyntacticRelation(from, wordMap.get(from), posMap.get(from),
                label, to, wordMap.get(to), posMap.get(to));
        newRelation.virtual = true;
        relations.add(newRelation);
    }

    /* for negraising, currently disabled

    void negate (Integer index, SyntacticRelationSet relations) {
        for (SyntacticRelation r : relations) {
            if (r.sourcePosn == index) {
                r.sourceWord = "not_" + getLemma(r.sourcePosn);
            }
            if (r.targetPosn == index) {
                r.targetWord = "not_" + getLemma(r.targetPosn);
            }
        }
    }

    void neutralize (Integer index, SyntacticRelationSet relations) {
        for (SyntacticRelation r : relations) {
            if (r.sourcePosn == index) {
                r.sourceWord = "do";
            }
            if (r.targetPosn == index) {
                r.targetWord = "do";
            }
        }
    }

    */

    void relabel (Integer from, Integer to, String label) {
	SyntacticRelation r = relations.getRelation(from, to);
	r.type = label;
    }

    void relabelNode (Integer i, String word) {
        wordMap.put(i, word);
        for (int j=0; j<relations.size(); j++) {
            SyntacticRelation r = (SyntacticRelation) relations.get(j);
            if (r.sourcePosn == i) r.sourceWord = word;
            if (r.targetPosn == i) r.targetWord = word;
        }
    }

    void replaceAll (Integer old, Integer nu) {
	String nuWord = wordMap.get(nu);
	for (int j=0; j<relations.size(); j++) {
	    SyntacticRelation r = (SyntacticRelation) relations.get(j);
	    if (r.sourcePosn == old) {
		r.sourcePosn = nu;
		r.sourceWord = nuWord;
	    }
	    if (r.targetPosn == old) {
		r.targetPosn = nu;
		r.targetWord = nuWord;
	    }
	}
    }

    Set<Integer> findInternalNodes (SyntacticRelationSet relations, Span span) {
        Set<Integer> internalNodes = new HashSet<Integer>();
        for (int j=0; j<relations.size(); j++) {
            SyntacticRelation r = (SyntacticRelation) relations.get(j);
            if (span == null ||
                    (r.sourcePosn >= span.start() && r.sourcePosn <= span.end()))
                internalNodes.add(r.sourcePosn);
        }
        return internalNodes;
    }

    /**
     *  returns 'true' if 'word' is an inflected form of 'be'.
     */

    public static boolean isBe(String word) {
	return (word.equals("be")) || (word.equals("being")) || (word.equals("been")) || (word.equals("am")) 
	    || (word.equals("'m")) || (word.equals("is")) || (word.equals("'s")) || (word.equals("are")) 
	    || (word.equals("'re")) || (word.equals("was")) || (word.equals("were"));
    }

    /**
     *  returns 'true' if 'word' is an inflected form of 'have'.
     */

    public static boolean isHave (String word) {
	return (word.equals("have")) || (word.equals("has")) || (word.equals("had")) || (word.equals("having")) ;
    }

    void transformTrace (String construct) {
	if (trace) {
	    System.out.println ("---Found " + construct);
	    System.out.println(relations.toString());
	}
    }

    /**
     *  a simple transformation tester:  reads in lines from standard input
     *  each containing a single sentence, parses the sentence,
     *  applies all transformations, and displays the result.  To quit the
     *  program, enter an empty line (just a return).
     */

    public static void main (String[] args) throws IOException {
	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	String propertyFile = args[0];
	JetTest.initializeFromConfig (propertyFile);
	DepTransformer transformer = new DepTransformer("trace");
	while (true) {
	    System.out.print ("sentence>");
	    String sentence = reader.readLine();
	    if (sentence.trim().length() == 0)
		break ;
	    Document doc = new Document(sentence + " ");
	    Control.processDocument (doc, null, false, 0);
	    // transform (and display)
	    doc.relations = transformer.transform(doc.relations, doc.fullSpan());
	}
    }
	
}
