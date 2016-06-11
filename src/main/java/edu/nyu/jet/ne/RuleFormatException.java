// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;

public class RuleFormatException extends Exception {
	public RuleFormatException() {
		this(null);
	}

	public RuleFormatException(String msg) {
		super(msg);
	}
}
