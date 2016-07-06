// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;


import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * Abstract Pipe class for adds feature value which is boolean value.
 *
 * @author Akira ODA
 */
public abstract class BooleanFeature extends Pipe {
	private String featureName;

	public BooleanFeature(String featureName) {
		this.featureName = featureName;
	}

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence tokens = (TokenSequence) carrier.getData();

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			String word = token.getText();

			if (matches(word)) {
				token.setFeatureValue(featureName, 1.0);
			}
		}

		return carrier;
	}

	/**
	 * Returns if specified word is matched condition. This method must be
	 * override in concrete subclass.
	 *
	 * @param word
	 * @return
	 */
	public abstract boolean matches(String word);
}
