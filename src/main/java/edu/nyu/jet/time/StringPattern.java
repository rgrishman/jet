// -*- tab-width: 4 -*-
package edu.nyu.jet.time;

import java.util.List;

import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;

public class StringPattern extends PatternItem {
	private String str;

	public StringPattern(String str) {
		this.str = str;
	}

	public PatternMatchResult match(Document doc, List<Annotation> tokens, int offset) {
		Annotation token = tokens.get(offset);
		String tokenStr = doc.normalizedText(token);

		if (tokenStr.equals(str)) {
			return new PatternMatchResult(tokenStr, token.span());
		} else {
			return null;
		}
	}
}
