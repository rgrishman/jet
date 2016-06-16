// -*- tab-width: 4 -*-
package edu.nyu.jet.ne;

public class ExactMatchClassHierarchyResolver implements
		ClassHierarchyResolver {

	public boolean isSubClassOf(String target, String className) {
		return className.equals(target);
	}
}
