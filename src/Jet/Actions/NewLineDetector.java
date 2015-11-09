package Jet.Actions;

import Jet.Lisp.FeatureSet;
import Jet.Tipster.Annotation;
import Jet.Tipster.Document;
import Jet.Tipster.Span;

import java.util.List;

/**
 * Split sentence when a token ends with two or more new line characters
 *
 * @author yhe
 * @version 1.0
 */
public class NewLineDetector implements JetAction {

    private boolean initialized = false;
    private int count = 1;
    private String newLineSequence = "\n";

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    public void initialize(String param) {
        try {
	    if (param != null)
            	count = Integer.valueOf(param);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append('\n');
            }
            newLineSequence = sb.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    @Override
    public void process(Document doc, Span span) {

        List<Annotation> sentences = doc.annotationsOfType("sentence", span);
        if (sentences == null) {
            return;
        }
        for (Annotation sentence : sentences) {
            List<Annotation> tokens = doc.annotationsOfType("token", sentence.span());
            if (tokens == null) {
                continue;
            }
            int start = sentence.start();
            for (Annotation token : tokens) {
                if (doc.text(token).contains(newLineSequence) || token.end() == sentence.end()) {
                    Annotation newSentence = new Annotation("sentence",
                            new Span(start, token.end()),
                            new FeatureSet());
                    doc.addAnnotation(newSentence);
                    start = token.end();
                }
            }
            doc.removeAnnotation(sentence);
        }
    }

    @Override
    public void process(Document doc) {
        process(doc, doc.fullSpan());
    }
}
