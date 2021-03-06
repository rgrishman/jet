// -*- tab-width: 4 -*-
//Title:        JET
//Version:      1.00
//Copyright:    Copyright (c) 2000
//Author:       Ralph Grishman
//Description:  A Java-based Information Extraction Tool

package edu.nyu.jet.lisp;

/**
 *  representation of a variable, as used in patterns.
 */

public class Variable {

  public String name;

  public Variable (String stg) {
    name = stg;
  }

  @Override
public String toString () {
    return "?" + name;
  }
}
