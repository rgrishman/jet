// -*- tab-width: 4 -*-
package Jet;

import AceJet.Datum;
import opennlp.tools.ml.AbstractTrainer;
import opennlp.tools.ml.maxent.io.ObjectQNModelReader;
import opennlp.tools.ml.maxent.io.ObjectQNModelWriter;
import opennlp.tools.ml.maxent.io.QNModelWriter;
import opennlp.tools.ml.maxent.quasinewton.QNModel;
import opennlp.tools.ml.maxent.quasinewton.QNTrainer;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.FileEventStream;
import opennlp.tools.util.ObjectStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * a wrapper for the maximum entropy code provided in the OpenNLP package.
 * modified by Yifan He in 2014 to optionally call Mallet max ent.
 */

public class QNMaxEntModel {

    String featureFileName;
    String modelFileName;
    PrintStream featureWriter = null;
    QNModel model = null;
    double l1cost = 0.0;
    double l2cost = 10;
    /**
     *  if true, create model with L2 regularization using Mallet;
     *  if false, use OpenNLP to create model (no regularization)
     */
    int cutoff = 4;
    int iterations = 100;

    /**
     * creates a new maximum entropy model.
     */

    public QNMaxEntModel() {
    }

    public void setRegularizationCost(double l1cost, double l2cost) {
        this.l1cost = l1cost;
        this.l2cost = l2cost;
    }

    /**
     * creates a new maximum entropy model, specifying files for both
     * the features and the resulting model.
     *
     * @param featureFileName the name of the file in which features will be
     *                        stored during training
     * @param modelFileName   the name of the file in which the max ent
     *                        model will be stored
     */

    public QNMaxEntModel(String featureFileName, String modelFileName) {
        this.featureFileName = featureFileName;
        this.modelFileName = modelFileName;
    }

    public void initializeForTraining(String featureFileName) {
        this.featureFileName = featureFileName;
        initializeForTraining();
    }

    public void initializeForTraining() {
        if (featureFileName == null) {
            System.out.println("MaxEntModel.initializeForTraining: no featureFileName specified");
        } else {
            try {
                featureWriter = new PrintStream(new FileOutputStream(featureFileName));
            } catch (IOException e) {
                System.out.print("Unable to create feature file: ");
                System.out.println(e);
            }
        }
    }

    /**
     * invoked during training to add one training Datum <CODE>d</CODE> to the
     * training set.
     */

    public void addEvent(Datum d) {
        if (featureWriter == null)
            initializeForTraining();
        featureWriter.println(d.toString2());
    }

    /**
     * sets the feature cutoff.  Features occurring fewer than <CODE>cutoff</CODE>
     * times in the training set are ignored.  Default value is 4.
     */

    public void setCutoff(int cutoff) {
        this.cutoff = cutoff;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public void buildModel() {
        boolean USE_SMOOTHING = false;
        double SMOOTHING_OBSERVATION = 0.1;
        boolean PRINT_MESSAGES = true;
        try {
            featureWriter.close();
            // FileReader datafr = new FileReader(new File(featureFileName));
            ObjectStream<Event> es =
                    new FileEventStream(featureFileName);
            // GIS.SMOOTHING_OBSERVATION = SMOOTHING_OBSERVATION;
            // as new OpenNLP uses L2 by default, no need for this distinctino here.
            // consider adding L1/L2/training algorithm choice in the future.
//	    if (USE_L2)
//                model = GIS.trainL2Model(es, 0, 2);
//	    else
            // model = GIS.trainModel(es, iterations, cutoff, USE_SMOOTHING, PRINT_MESSAGES);
            // QNTrainer trainer = new QNTrainer();
            Map<String, String> trainParams = new HashMap<String, String>();
            trainParams.put(AbstractTrainer.CUTOFF_PARAM, Integer.toString(1));
            trainParams.put(QNTrainer.L1COST_PARAM, Double.toString(l1cost));
            trainParams.put(QNTrainer.L2COST_PARAM, Double.toString(l2cost));
            trainParams.put(AbstractTrainer.ALGORITHM_PARAM, QNTrainer.MAXENT_QN_VALUE);
            Map<String, String> resultMap = new HashMap<String, String>();
            QNTrainer qnTrainer = new QNTrainer();
            qnTrainer.init(trainParams, resultMap);
            model = (QNModel) qnTrainer.train(es);
        } catch (Exception e) {
            System.out.print("Unable to create model due to exception: ");
            System.out.println(e);
        }
    }

    public void saveModel() {
        if (modelFileName == null) {
            System.out.println("MaxEntModel.saveModel:  no modelFileName specified");
        } else {
            saveModel(modelFileName);
        }
    }

    public void saveModel(String modelFileName) {
        try {
            File outputFile = new File(modelFileName);
            QNModelWriter modelWriter = new ObjectQNModelWriter(model,
                    new ObjectOutputStream(new FileOutputStream(outputFile)));
            //GISModelWriter modelWriter = new SuffixSensitiveGISModelWriter(model, outputFile);
            modelWriter.persist();
        } catch (IOException e) {
            System.out.print("Unable to save model: ");
            System.out.println(e);
        }
    }

    public void saveModel(BufferedWriter writer) {
        try {
            if (model == null) {
                System.err.println("Error: model is null.");
            }
            QNModelWriter modelWriter =  new ObjectQNModelWriter(model, new ObjectOutputStream(
                    new WriterOutputStream(writer)
            ));
            modelWriter.persist();
            // GISModelWriter modelWriter = new PlainTextGISModelWriter(model, writer);
            // modelWriter.persist();
        } catch (IOException e) {
            System.out.print("Unable to save model: ");
            System.out.println(e);
        }
    }

    public void saveModel(ObjectOutputStream oos) {
        try {
            if (model == null) {
                System.err.println("Error: model is null.");
            }
            QNModelWriter modelWriter =  new ObjectQNModelWriter(model, oos);
            modelWriter.persist();
            // GISModelWriter modelWriter = new PlainTextGISModelWriter(model, writer);
            // modelWriter.persist();
        } catch (IOException e) {
            System.out.print("Unable to save model: ");
            System.out.println(e);
        }
    }

    public void loadModel() {
        if (modelFileName == null) {
            System.out.println("MaxEntModel.loadModel:  no modelFileName specified");
        } else {
            loadModel(modelFileName);
        }
    }

    public void loadModel(String modelFileName) {
        try {
            File f = new File(modelFileName);
//            model = (GISModel) new SuffixSensitiveGISModelReader(f).getModel();
            ObjectQNModelReader reader =
                    new ObjectQNModelReader(new ObjectInputStream(new FileInputStream(f)));
            model = (QNModel) reader.getModel();
            System.out.println("MaxEnt model " + f.getName() + " loaded.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void loadModel(BufferedReader reader) {
        try {
            // model = (GISModel) new PlainTextGISModelReader(reader).getModel();
            ObjectQNModelReader r =
                    new ObjectQNModelReader(new ObjectInputStream(new ReaderInputStream(reader, Charsets.UTF_8)));
            model = (QNModel)r.getModel();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void loadModel(ObjectInputStream ois) {
        try {
            // model = (GISModel) new PlainTextGISModelReader(reader).getModel();
            ObjectQNModelReader r =
                    new ObjectQNModelReader(ois);
            model = (QNModel)r.getModel();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public boolean isLoaded() {
        return model != null;
    }

    /**
     * (for a trained model) returns the probability that the Datum
     * <CODE>d</CODE> is classified as <CODE>value</CODE>.
     */

    public double prob(Datum d, String value) {
        return model.eval(d.toArray())[model.getIndex(value)];
    }

    /**
     * (for a trained model) returns the most likely outcome for Datum
     * <CODE>d</CODE>.
     */

    public String bestOutcome(Datum d) {
        return model.getBestOutcome(model.eval(d.toArray())).intern();
    }

    public int getNumOutcomes() {
        return model.getNumOutcomes();
    }

    public String getOutcome(int i) {
        return model.getOutcome(i);
    }

    public double[] getOutcomeProbabilities(Datum d) {
        return model.eval(d.toArray());
    }
}
