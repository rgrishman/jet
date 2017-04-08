// -*- tab-width: 4 -*-
//Title:        JET
//Version:      1.81
//Copyright:    Copyright (c) 2015
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

package edu.nyu.jet.aceJet;

import java.util.*;
import java.io.*;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.tipster.*;
import edu.nyu.jet.zoner.SentenceSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  a relation tagger based on dependency paths and argument types,
 *  as produced by Jet ICE.
 */

public class DepPathRelationTagger {

	final static Logger logger = LoggerFactory.getLogger(DepPathRelationTagger.class);

	static Document doc;
	static AceDocument aceDoc;
	static String currentDoc;

	static int WINDOW = 5;

	// model:  a map from AnchoredPath strings to relation types
	static Map<String, List<String>> model = null;

	/**
	 *  relation 'decoder':  identifies the relations in document 'doc' 
	 *  (from file name 'currentDoc') and adds them
	 *  as AceRelations to AceDocument 'aceDoc'.
	 */

	public static void findRelations (String currentDoc, Document d, AceDocument ad) {
		doc = d;
		RelationTagger.doc = d;
		doc.relations.addInverses();
		aceDoc = ad;
		RelationTagger.docName = currentDoc;
		SentenceSet sentences = new SentenceSet(doc);
		RelationTagger.relationList = new ArrayList<AceRelation> ();
		// RelationTagger.findEntityMentions (aceDoc);
		AceMention[] ray = aceDoc.allMentionsList.toArray(new AceMention[0]);
		Arrays.sort(ray);
		for (int i = 0; i < ray.length - 1; i++) {
		    for (int j = 1; j < WINDOW && i + j < ray.length; j++) {
			AceMention m1 = ray[i];
			AceMention m2 = ray[i+j];
			// if two mentions co-refer, they can't be in a relation
			// if (!canBeRelated(m1, m2)) continue;
			// if two mentions are not in the same sentence, they can't be in a relation  
			if (!sentences.inSameSentence(m1.getJetHead().start(), m2.getJetHead().start())) continue;
			predictRelation(m1, m2, doc.relations);
		    }
		}
		// combine relation mentions into relations
		RelationTagger.relationCoref (aceDoc);
		RelationTagger.removeRedundantMentions (aceDoc);
	}

	/**
	 *  load the model used by the relation tagger.  Each line consists of an
	 *  AnchoredPath [a lexicalized dependency path with information on the
	 *  endpoint types], an equals sign, and a relation type.
	 */

	static void loadModel (String modelFile) throws IOException {
	    System.out.println("loading dep path relational model from file " + modelFile);
		model = new TreeMap<String, List<String>>();
		BufferedReader reader = new BufferedReader (new FileReader (modelFile));
		String line;
		int n = 0;
		int lineNo = 0;
		while ((line = reader.readLine()) != null) {
			lineNo++;
			if (line.startsWith("#"))
			    continue;
			String[] fields = line.split("=");
			if (fields.length < 2) {
			    loadError (lineNo, line, "missing =");
			    continue;
			}
			if (fields.length > 2) {
			    loadError (lineNo, line, "extra =");
			    continue;
			}
			String pattern = fields[0].trim();
			String outcome = fields[1].trim();
			if (!AnchoredPath.valid(pattern)) {
			    loadError (lineNo, line, "invalid path");
			    continue;
			}
                        if (model.get(pattern) == null)
                            model.put (pattern, new ArrayList<String>());
                        model.get(pattern).add(outcome);
                        n++;
		}
		System.out.println ("Loaded " + n + " dependency paths.");
	}

	private static void loadError (int lineNo, String line, String message) {
	    System.out.println(" *** Invalid dep path (" + message + ") on line " + lineNo + ":");
	    System.out.println("        " + line);
	}

	/**
	 *  use dependency paths to determine whether the pair of mentions bears some
	 *  ACE relation;  if so, add the relation to relationList.  If the path appears
         *  with multiple relation types, add each one to the list.
	 */

	private static void predictRelation (AceMention m1, AceMention m2,
			SyntacticRelationSet relations) {
		// compute path
		int h1 = m1.getJetHead().start();
		int h2 = m2.getJetHead().start();
		String path = EventSyntacticPattern.buildSyntacticPath(h1, h2, relations);
		if (path == null) return;
		path = AnchoredPath.reduceConjunction (path);
		if (path == null) return;
		path = AnchoredPath.lemmatizePath (path);
		// build pattern = path + arg types
		String pattern = m1.getType() + "--" + path + "--" + m2.getType();
		// look up path in model
		List<String> outcomes = model.get(pattern);
		if (outcomes == null) return;
		// if (!RelationTagger.blockingTest(m1, m2)) return;
		// if (!RelationTagger.blockingTest(m2, m1)) return;
                for (String outcome : outcomes) {
                    boolean inv = outcome.endsWith("-1");
                    outcome = outcome.replace("-1", "");
                    String[] typeSubtype = outcome.split(":", 2);
                    String type = typeSubtype[0];
                    String subtype;
                    if (typeSubtype.length == 1) {
			subtype = "";
                    } else {
                        subtype = typeSubtype[1];
                    }
                    if (inv) {
			AceRelationMention mention = new AceRelationMention("", m2, m1, doc);
                        System.out.println ("Found " + outcome + " relation " + mention.text);  //<<<
			AceRelation relation = new AceRelation("", type, subtype, "", m2.getParent(), m1.getParent());
			relation.addMention(mention);
			RelationTagger.relationList.add(relation);
                    } else {
                        AceRelationMention mention = new AceRelationMention("", m1, m2, doc);
                        System.out.println ("Found " + outcome + " relation " + mention.text);  //<<<
                        AceRelation relation = new AceRelation("", type, subtype, "", m1.getParent(), m2.getParent());
                        relation.addMention(mention);
                        RelationTagger.relationList.add(relation);
                    }
		}
	}

}
