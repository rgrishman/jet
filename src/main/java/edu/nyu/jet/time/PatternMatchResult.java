// -*- tab-width: 4 -*-
package edu.nyu.jet.time;

import edu.nyu.jet.tipster.Span;

public class PatternMatchResult {
	public Object value;
	public Span span;

	public PatternMatchResult(Object value, Span span) {
		this.value = value;
		this.span = span;
	}
}

