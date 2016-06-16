// -*- tab-width: 4 -*-
/**
 *
 */
package edu.nyu.jet.ne;

import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;

public class PartOfSpeechRule {
	private MatchType type;

	private String[] pos;

	public PartOfSpeechRule(MatchType type, String[] pos) {
		this.type = type;
		this.pos = pos;
	}

	public boolean accept(Document doc, Annotation[] tokens, int n) {
		if (type == MatchType.ANY) {
			return true;
		}

		String targetPos = (String) tokens[n].get("pos");
		if (targetPos == null) {
			return false;
		}

		boolean result = false;
		for (String p : pos) {
			if (targetPos.equals(p)) {
				result = true;
				break;
			}
		}

		switch (type) {
		case NORMAL:
			return result;

		case NOT:
			return !result;

		default:
			// unreachable
			throw new InternalError();
		}
	}
}
