package Jet.Parser;

import Jet.Tipster.*;
import Jet.Lisp.*;
import Jet.Parser.DepParser;
import Jet.Parser.DepTransformer;
import Jet.Parser.SyntacticRelation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.BeforeClass;

/**
 * A basic functionality test for the dependency parser.
 */

public class DepParserTest {

    @BeforeClass 
    public static void loadGrammar() {
	DepParser.initWrapper("/home/grishman/jetx/jet/data/parseModel.gz");
    }

    Document doc;

    DepTransformer transformer = new DepTransformer("trace");

    // simple sentence (no traansformations apply)

    @Test 
    public void testParser1 () {
        doc = new Document();
        int my = sword("my", "PRP$");
        int banker = sword("banker", "NN");
        int sells = sword("sells", "VBZ");
        int bonds = sword("bonds", "NNS");
        int period = sword(".", ".");
        DepParser.parseSentence (doc, doc.fullSpan(), doc.relations);
	checkDep (sells, "nsubj", banker);
	checkDep (sells, "dobj", bonds);
	transformer.transform(doc, doc.fullSpan());
	checkDep (sells, "nsubj", banker);
    }

    // perfect tense
 
    @Test 
    public void testParser2 () {
        doc = new Document();
        int my = sword("my", "PRP$");
        int banker = sword("banker", "NN");
        int has = sword("has", "VBZ");
        int sold = sword("sold", "VBN");
        int bonds = sword("bonds", "NNS");
        int period = sword(".", ".");
        DepParser.parseSentence (doc, doc.fullSpan(), doc.relations);
	checkDep (has, "nsubj", banker);
	transformer.transform(doc, doc.fullSpan());
	checkDep (sold, "nsubj", banker);
    }

    // nested clauses (no transformations apply)

    @Test 
    public void testParser3 () {
        doc = new Document();
        int she = sword("she", "PRP");
        int knows = sword("knows", "VBZ");
        int what = sword("what", "WP");
        int he = sword("he", "PRP");
        int wants = sword("wants", "VBZ");
        int period = sword(".", ".");
        DepParser.parseSentence (doc, doc.fullSpan(), doc.relations);
	System.out.println (doc.relations);
	checkDep (knows, "ccomp", wants);
    }

    int sword (String word, String pos) {
        int start = doc.length();
        doc.append(word + " ");
        int end = doc.length();
        Span span = new Span(start, end);
        doc.annotate("token", span, null);
        doc.annotate("tagger", span, new FeatureSet("cat", pos));
        return start;
    }   
    
    // verify that relation 'type' holds between nodes 'from' and 'to'
 
    void checkDep (int from, String type, int to) {
	SyntacticRelation r = doc.relations.getRelation(from, to);
	assertNotNull("missing dependency from " + from + " to " + to, r);
	assertEquals("dependency from " + from + " to " + to +
	    " is of type " + r.type + " should be " + type, r.type, type);
    }
}   
