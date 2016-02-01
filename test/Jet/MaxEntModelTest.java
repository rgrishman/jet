package Jet;

import AceJet.Datum;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the MaxEntModel class using a very small training collection
 *
 * @author yhe
 * @version 1.0
 */
public class MaxEntModelTest {

    @Test
    public void testBuildModel() throws Exception {
        QNMaxEntModel model = new QNMaxEntModel();
        model.initializeForTraining("feats");
        Datum d = new Datum();
        d.addF("rain-1");
        d.addF("cloudy-2");
        d.setOutcome("rain");
        model.addEvent(d);

        d = new Datum();
        d.addF("rain-1");
        d.addF("sunny-2");
        d.setOutcome("rain");
        model.addEvent(d);

        d = new Datum();
        d.addF("cloudy-1");
        d.addF("sunny-2");
        d.setOutcome("cloudy");
        model.addEvent(d);

        d = new Datum();
        d.addF("cloudy-1");
        d.addF("cloudy-2");
        d.setOutcome("cloudy");
        model.addEvent(d);

        model.buildModel();

        model.modelFileName = "model";

        model.saveModel();

        // test model output

        model = new QNMaxEntModel();
        model.loadModel("model");

        d = new Datum();
        d.addF("rain-1");
        d.addF("cloudy-2");
        d.setOutcome("UNK");
        System.out.println(model.bestOutcome(d));
        d = new Datum();
        d.addF("cloudy-1");
        d.addF("cloudy-2");
        d.setOutcome("UNK");
        System.out.println(model.bestOutcome(d));
    }
}