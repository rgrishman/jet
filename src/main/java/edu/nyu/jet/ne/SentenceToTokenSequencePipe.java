// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;

import java.util.Collections;
import java.util.List;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.Span;

public class SentenceToTokenSequencePipe extends Pipe {
	public SentenceToTokenSequencePipe() {
		super(null, new LabelAlphabet());
	}

	@Override
	public Instance pipe(Instance carrier) {
		Document doc = (Document) carrier.getSource();
		Span span = (Span) carrier.getData();

		List<Annotation> names = doc.annotationsOfType("ENAMEX", span);
		if (names == null) {
			names = Collections.emptyList();
		}
		Annotation.sortByStartPosition(names);

		TokenSequence data = new TokenSequence();
		LabelSequence target = new LabelSequence(
				getTargetAlphabet());

		int pos = span.start();
		for (Annotation name : names) {
			if (name.start() > pos) {
				addTokens(data, target, doc, new Span(pos, name.start()), "O");
			}
			addTokens(data, target, doc, name.span(), (String) name.get("TYPE"));
			pos = name.end();
		}

		if (pos < span.end()) {
			addTokens(data, target, doc, new Span(pos, span.end()), "O");
		}

		carrier.setData(data);
		carrier.setSource(data);
		carrier.setTarget(target);
		carrier.setProperty("document", doc);
		carrier.setProperty("span", span);

		return carrier;
	}

	private void addTokens(TokenSequence data, LabelSequence target,
			Document doc, Span span, String label) {
		List<Annotation> tokens = doc.annotationsOfType("token", span);
		if (tokens == null) {
			return;
		}

		Annotation.sortByStartPosition(tokens);

		if (label.equals("O")) {
			// out of named entity
			for (Annotation token : tokens) {
				data.add(makeToken(doc, token));
				target.add(label);
			}
		} else {
			// in named entity
			data.add(makeToken(doc, tokens.get(0)));
			target.add("B-" + label);

			String followingLabel = "I-" + label;
			for (int i = 1; i < tokens.size(); i++) {
				Annotation token = tokens.get(i);
				data.add(makeToken(doc, token));
				target.add(followingLabel);
			}
		}
	}

	private Token makeToken(Document doc, Annotation token) {
		Token t = new Token(doc.normalizedText(token));
		t.setProperty("span", token.span());
		return t;
	}
}
