package edu.nyu.jet.parser;

import static org.junit.Assert.assertEquals;

import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;

import edu.nyu.jet.tipster.*;
import edu.nyu.jet.lisp.*;

import junit.framework.JUnit4TestAdapter;
import org.junit.Test;

public class DepAnalyzerTest {

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(DepAnalyzerTest.class);
    }

    @Test
    public void testFindApposite () throws Exception {

        SyntacticRelationSet relations = new SyntacticRelationSet();
        String s = 
            "poss | father | 3 | NN | My | 0 | PRP$ \n" +  
            "dobj | named | 27 | VBD | father | 3 | NN \n" +
            "punct | father | 3 | NN | , | 9 | , \n" +
            "appos | father | 3 | NN | Fred_Smith | 11 | NNP \n" +
            "punct | father | 3 | NN | , | 21 | , \n" +
            "objcomp | named | 27 | VBD | president | 33 | NN \n" +
            "punct | named | 27 | VBD | . | 42 | . \n";
        relations.read(new BufferedReader (new StringReader(s)));

        Document doc =
            new Document("My father, Fred Smith, was named president. ");
        Vector<Annotation> mentions = new Vector<Annotation>();
        Annotation h1 = new Annotation("constit", new Span(3, 9), new FeatureSet("cat", "n"));
        doc.addAnnotation(h1);
        Annotation m1 = new Annotation("constit", new Span(0, 9), new FeatureSet("cat", "np", "headC", h1));
        doc.addAnnotation(m1);
        mentions.add(m1);
        Annotation m2 = new Annotation("constit", new Span(11, 21),  new FeatureSet("cat", "np"));
        doc.addAnnotation(m2);
        mentions.add(m2);

        DepAnalyzer.convertRelations (doc, relations, mentions);

        // check apposite feature of first mention
        assertEquals(m1.get("apposite"), m2);
    }
}
