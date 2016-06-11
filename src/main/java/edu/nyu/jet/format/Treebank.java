// -*- tab-width: 4 -*-
package edu.nyu.jet.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.nyu.jet.parser.ParseTreeNode;
import edu.nyu.jet.tipster.Document;

public class Treebank {
	private Document document;

	private List<ParseTreeNode> parseTreeList;

	public Treebank(Document doc, List<ParseTreeNode> parseTreeList) {
		this.document = doc;
		this.parseTreeList = parseTreeList;
	}

	public Document getDocument() {
		return document;
	}

	public List<ParseTreeNode> getParseTreeList() {
		return parseTreeList;
	}

	public ParseTreeNode getParseTree(int i) {
		return parseTreeList.get(i);
	}
}
