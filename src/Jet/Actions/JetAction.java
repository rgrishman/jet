package Jet.Actions;

import Jet.Tipster.Document;
import Jet.Tipster.Span;

/**
 * User defined actions in Jet.
 *
 * @author yhe
 * @version 1.0
 */
public interface JetAction {
    boolean initialized();
    void initialize(String param);
    void process(Document doc, Span span);
    void process(Document doc);
}
