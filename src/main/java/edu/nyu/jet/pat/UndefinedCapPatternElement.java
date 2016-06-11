// -*- tab-width: 4 -*-
//Title:        JET
//Version:      1.03
//Copyright:    Copyright (c) 2001
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

package edu.nyu.jet.pat;

import edu.nyu.jet.lisp.*;
import edu.nyu.jet.tipster.*;
import edu.nyu.jet.Console;
import java.util.*;

public class UndefinedCapPatternElement extends AtomicPatternElement {

  public UndefinedCapPatternElement(FeatureSet fs) {
  }

  public void eval(Document doc, int posn, String tokenString,
        HashMap bindings, PatternApplication patap, PatternNode node) {
    Annotation token = doc.tokenAt(posn);
    if (token == null) return;
    if (token.get("hidden") != null) return;
    Object tokenCase = token.get("case");
    if (!(tokenCase == "cap" || tokenCase == "forcedCap")) return;
    if (doc.annotationsAt(posn,"constit") != null) return;
    int ic = token.span().end();
    node.eval(doc, ic, bindings, patap);
    }

  public String toString () {
    return "[undefinedCap]";
  }
}
