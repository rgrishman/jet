// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenLowerText extends Pipe {
	private String prefix;

	public TokenLowerText(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence tokens = (TokenSequence) carrier.getData();

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			String name = prefix + token.getText().toLowerCase();
			token.setFeatureValue(name, 1.0);
		}

		return carrier;
	}
}
