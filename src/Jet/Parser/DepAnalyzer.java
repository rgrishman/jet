// -*- tab-width: 4 -*-
//Title:        JET
//Version:      1.80
//Copyright:    Copyright (c) 2015
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

package Jet.Refres;

import java.util.*;
import java.io.*;

import Jet.Lisp.*;
import Jet.Tipster.*;
import Jet.Parser.SyntacticRelation;
import Jet.Parser.SyntacticRelationSet;
import Jet.Parser.SynFun;
import Jet.Refres.Resolve;

/**
 *  Check for selected relations in dependency parse and add corresponding features to
 *  annotations.  Currently checks for apposition, predicate complement, and 'of' PPs.
 */

public class DepAnalyzer {

    static Map<Integer, Annotation> head2mention = new HashMap<Integer, Annotation>();

    /**
     *  Check for relations in Document <CODE>doc</CODE> with dependency relations
     *  <CODE>relations</CODE> and entity mentions <CODE>mentions</CODE>.
     */

    static void convertRelations (Document doc, SyntacticRelationSet relations, Vector<Annotation> mentions) {
        //
        //  add pointers from dependency nodes to constituents
        //
        for (Annotation m : mentions) {
            int headPosn = Resolve.getHeadC(m).start();
            head2mention.put(headPosn, m);
        }
        //
        //  add apposite links based on info from dependency parse
        //
        for (Annotation m : mentions) {
            int headPosn = Resolve.getHeadC(m).start();
            SyntacticRelation r = relations.getRelation(headPosn, "appos");
            if (r != null) {
                int appositeHead = r.targetPosn;
                Annotation appositeMention = head2mention.get(appositeHead);
                if (appositeMention == null) continue;
                m.put("apposite", appositeMention);
                List<Integer> conjuncts = getConjuncts(relations, appositeHead);
                if (!conjuncts.isEmpty()) {
                    int conjunctHead = conjuncts.get(0);
                    Annotation conjunctMention = head2mention.get(conjunctHead);
                    if (conjunctMention == null) continue;
                    appositeMention.put("conjunct", conjunctMention);
                }
            }
        }
        //
        //  add "of" links based on info from dependency parse
        //
        for (Annotation m : mentions) {
            Integer pobjHead = objectOfOf(m, relations);
            if (pobjHead != null) {
                Annotation pobjMention = head2mention.get(pobjHead);
                if (pobjMention == null) continue;
                m.put("of", pobjMention);
                List<Integer> conjuncts = getConjuncts(relations, pobjHead);
                if (!conjuncts.isEmpty()) {
                    int conjunctHead = conjuncts.get(0);
                    Annotation conjunctMention = head2mention.get(conjunctHead);
                    if (conjunctMention == null) continue;
                    pobjMention.put("conjunct", conjunctMention);
                }
            }
        }
        //
        //  add predComp links
        //
        for (Annotation m : mentions) {
            int headPosn = Resolve.getHeadC(m).start();
            SyntacticRelation r = relations.getRelationTo(headPosn);
            if (r != null && r.type.equals("nsubj")) {
                int verbPosn = r.sourcePosn;
                SyntacticRelation r2 = relations.getRelation(verbPosn, "cop");
                if (r2 != null) {
                    int copulaHead = r2.targetPosn;
                    Annotation copulaMention = head2mention.get(copulaHead);
                    if (copulaMention != null) {
                        m.put("predComp", copulaMention);
                        List<Integer> conjuncts = getConjuncts(relations, copulaHead);
                        if (!conjuncts.isEmpty()) {
                            int conjunctHead = conjuncts.get(0);
                            Annotation conjunctMention = head2mention.get(conjunctHead);
                            if (conjunctMention == null) continue;
                            copulaMention.put("conjunct", conjunctMention);
                        }
                    }
                }
            }
        }
    }

    /**
     *  If the dependency structure has an 'of' PP modifying constituent annotation
     *  <CODE>m</CODE>, returns the position of the object of the 'of';
     *  otherwise returns <CODE>null</CODE>.  Both full (two arc) and reduced
     *  forms (one arc) of the PP are recognized.
     */

    public static Integer objectOfOf (Annotation m, SyntacticRelationSet relations) {
        int headPosn = Resolve.getHeadC(m).start();
        SyntacticRelation r = relations.getRelation(headPosn, "prep_of");
        if (r != null) {
            return r.targetPosn;
        }
        SyntacticRelationSet preps = relations.getRelations(headPosn, "prep");
        for (SyntacticRelation prep : preps) {
            if (prep.targetWord.equals("of")) {
                int posnOfOf = prep.targetPosn;
                SyntacticRelation s = relations.getRelation(posnOfOf, "pobj");        
                if (s != null) {
                    return s.targetPosn;
                }
            }
        }
        return null;
    }

    /**
     *  given the position 'p' of the first conjunct of a conjoined value,
     *  returns a list of the positions of the other conjuncts.  If p is
     *  not conjoined, returns an empty list.
     */

    public static List<Integer> getConjuncts (SyntacticRelationSet relations, int p) {
        List<Integer> conjuncts = new ArrayList<Integer>();
        SyntacticRelationSet srSet = relations.getRelationsFrom(p);
        for (int j = 0; j < srSet.size(); j++) {
            SyntacticRelation r = srSet.get(j);
            if (r.type.equals("cc")) {
                int ccPosn = r.targetPosn;;
                SyntacticRelationSet srSet2 = relations.getRelationsFrom(ccPosn);
                for (int k = 0; k < srSet2.size(); k++) {
                    SyntacticRelation r2 = srSet2.get(k);
                    if (r2.type.equals("conj")) {
                        int conjPosn = r2.targetPosn;
                        conjuncts.add(conjPosn);
                    }
                }
            }
        }
        return conjuncts;
    }
}
