// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class FirstWordFeature extends Pipe {
	private String name;

	public FirstWordFeature(String name) {
		this.name = name;
	}

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence tokens = (TokenSequence) carrier.getData();
		if (tokens.size() > 0) {
			Token token = tokens.get(0);
			token.setFeatureValue(name, 1.0f);
		}
		return carrier;
	}
}
