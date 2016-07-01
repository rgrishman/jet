// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import edu.nyu.jet.lex.Lexicon;
import edu.nyu.jet.lisp.FeatureSet;

public class LexiconCategoryFeature extends Pipe {
	private String prefix;

	public LexiconCategoryFeature(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Instance pipe(Instance carrier) {
		TokenSequence tokens = (TokenSequence) carrier.getData();
		for (int i = 0 ; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			String word = token.getText().toLowerCase();
			FeatureSet[] definitions = Lexicon.lookUp(new String[] { word } );

			if (definitions != null) {
				for (FeatureSet fs : definitions) {
					String name = prefix + fs.get("cat");
					token.setFeatureValue(name.intern(), 1.0);
				}
			}
		}

		return carrier;
	}

}
