// -*- tab-width: 4 -*-
/**
 *
 */
package edu.nyu.jet.ne;

import edu.nyu.jet.tipster.Annotation;
import edu.nyu.jet.tipster.Document;

public interface MatchRule {
	public boolean accept(Document doc, Annotation[] tokens, int n);
}
