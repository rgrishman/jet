// -*- tab-width: 4 -*-
package edu.nyu.jet.scorer;

import java.io.*;
import edu.nyu.jet.tipster.*;
import edu.nyu.jet.hmm.HMMannotator;

public interface NameTagger {

	public void tagDocument (Document doc);

	public void tag (Document doc, Span span);

	public void load (String fileName) throws IOException;

	public void newDocument ();

}
