// -*- tab-width: 4 -*-
package edu.nyu.jet.parser;

import edu.nyu.jet.tipster.*;
import edu.nyu.jet.lisp.*;

import java.util.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *  A graphical display of a dependency tree.
 */

public class DepParseView extends JFrame {

    private JScrollPane jScrollPane = new JScrollPane();
    private JParseComponent parseComponent;
    private SyntacticRelationSet relations;

    /**
     *  creates a new Frame entitled 'title' displaying the 
     *  dependency relations in <CODE>relations</CODE>.
     */

    public DepParseView(String title, SyntacticRelationSet relations) {
	super (title);
	setSize (400, 300);
	parseComponent = new JParseComponent();
	jScrollPane.getViewport().add(parseComponent);
	getContentPane().add(jScrollPane, BorderLayout.CENTER);
	this.setVisible(true);
	this.relations = relations;

	// find root (s):  nodes that are never targets
	TreeSet<Integer> roots = new TreeSet<Integer>();
	TreeSet<Integer> targets = new TreeSet<Integer>();
	for (SyntacticRelation r : relations) {
	    roots.add(r.sourcePosn);
	    targets.add(r.targetPosn);
	}
	roots.removeAll(targets);
	if (roots.isEmpty()) {
	    System.out.println("No parse to display.");
	    return;
	}
	int rootPosn = roots.first();
	String rootWord = "?";
	for (SyntacticRelation r : relations) {
	    if (r.sourcePosn == rootPosn) {
		rootWord = r.sourceWord;
	    }
	}

	parseComponent.init (rootPosn, rootWord);
    }

  /**
   *  the JComponent within the ParseView which actually displays the tree.
   */

    protected class JParseComponent extends JComponent {

	// a list of all the nodes in the tree
  	ArrayList<Integer> nodes = new ArrayList<Integer>();
  	// the (x, y) position of each node in the tree
	Map<Integer, Point> position = new HashMap<Integer, Point>();
	Map<Integer, String> nodeName = new HashMap<Integer, String>();
	FontMetrics fm;
	private int height;
	private int verticalSeparation;
	private int verticalDisplacement;
	private int horizontalSeparation;
	private int maxX, maxY;
	// the root of the tree
	private int rootPosn;
	private String rootWord;

  	JParseComponent () {
	}

	/**
	 *  initialize the tree:  compute the position of each node.
	 */

  	void init (int rootPosn, String rootWord) {
	    fm = getGraphics().getFontMetrics();
	    horizontalSeparation = fm.charWidth('m');
	    height = fm.getHeight();
	    verticalSeparation = height * 2;
	    verticalDisplacement = height + 2 * verticalSeparation;
	    this.rootPosn = rootPosn;
	    this.rootWord = rootWord;
	    layOutTree();
	}

	void layOutTree () {
	    maxX = 0;
	    maxY = 0;
	    computePosition(rootPosn, rootWord, 10, 10);
	    setPreferredSize(new Dimension(maxX + 50, maxY + 50));
	}

  	/**
  	 *  compute the position of the subtree rooted at 'rootPosn', where 'x' and 'y'
  	 *  are the coordinates of the upper left hand corner of the rectangle
  	 *  within which 'rootPosn' and its subtree will be displayed.  The
  	 *  position of 'rootPosn' is the position of the horizontal center
  	 *  of the baseline of the name of node root.
  	 */

	int computePosition (int rootPosn, String rootWord, int x, int y) {
	    if (nodes.contains(rootPosn))
		return 0;
	    nodes.add(rootPosn);
	    nodeName.put(rootPosn, rootWord);
	    int childrenWidth = 0;
	    SyntacticRelationSet children = relations.getRelationsFrom(rootPosn);
	    if (children != null) {
		for (SyntacticRelation child : children) {
		    int w = computePosition(child.targetPosn, child.targetWord, 
			    x+childrenWidth, y + verticalDisplacement) + horizontalSeparation;
		    int typeW = 2 * (fm.stringWidth(child.type) + horizontalSeparation);
		    w = Math.max(w, typeW);
		    childrenWidth += w;
		}
		childrenWidth -= horizontalSeparation;
	    }
	    int localWidth = fm.stringWidth(rootWord);
	    int width = Math.max(childrenWidth, localWidth);
	    position.put(rootPosn, new Point(x + (width / 2), y));
	    maxX = Math.max(maxX, x);
	    maxY = Math.max(maxY, y);
	    return width;
	}

	  /**
	   *  draw the parse tree (once the positions of the nodes have
	   *  been calcuated by 'init'.
	   */

	  public void paintComponent (Graphics g) {
	      super.paintComponent(g);
	      for (int node : nodes) {
		  Point p = (Point) position.get(node);
		  String name = nodeName.get(node);
		  int nodeWidth = fm.stringWidth(name);
		  g.drawString(name, p.x-(nodeWidth/2), p.y);
		  SyntacticRelationSet children = relations.getRelationsFrom(node);
		  if (children != null) {
		      for (SyntacticRelation child : children) {
			  Point pChild = position.get(child.targetPosn);
			  boolean downwards = pChild.y > p.y;
			  if (downwards) {
			      g.drawLine(p.x, p.y+2, pChild.x, pChild.y-height);
			      g.setColor(Color.red);
			      g.drawString(child.type, (p.x + pChild.x) / 2, 
				      (p.y + 2 + pChild.y - height) / 2);
			      g.setColor(Color.black);
			  } else {
			      // upward links are
			      // * colored blue
			      // * displaced 5 pixels right of downward links
			      // * have labels placed lower
			      g.setColor(Color.blue);
			      g.drawLine(p.x + 5, p.y-height, pChild.x + 5, pChild.y+2);
			      g.drawString(child.type, (p.x + pChild.x) / 2, 
				      (p.y + 2 + pChild.y + height) / 2);
			      g.setColor(Color.black);
			  }
		      }
		  }
	      }
	  }
    }
}
