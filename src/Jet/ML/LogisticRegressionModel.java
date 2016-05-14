package Jet.ML;

import AceJet.Datum;
import Jet.MaxEntModel;
import edu.emory.mathcs.nlp.learning.optimization.method.AdaGrad;
import edu.emory.mathcs.nlp.learning.util.FeatureVector;
import edu.emory.mathcs.nlp.learning.util.Instance;
import edu.emory.mathcs.nlp.learning.util.SparseVector;
import edu.emory.mathcs.nlp.learning.util.WeightVector;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import opennlp.maxent.BasicEventStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.*;
import opennlp.model.Event;
import opennlp.model.EventStream;
import opennlp.model.OnePassDataIndexer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by yhe on 5/8/16.
 */
public class LogisticRegressionModel extends MaxEntModel {
    Object2IntMap<String> predMap;
    Object2IntMap<String> outcomeMap;
    WeightVector w;
    String[] outcomeLabels;

    /**
     * creates a new maximum entropy model, specifying files for both
     * the features and the resulting model.
     *
     * @param featureFileName the name of the file in which features will be
     *                        stored during training
     * @param modelFileName   the name of the file in which the max ent
     *                        model will be stored
     */

    public LogisticRegressionModel(String featureFileName, String modelFileName) {
        this.featureFileName = featureFileName;
        this.modelFileName = modelFileName;
    }




    public void buildModel() {
        try {
            featureWriter.close();
            FileReader datafr = new FileReader(new File(featureFileName));
            EventStream es =
                    new BasicEventStream(new PlainTextByLineDataStream(datafr));
            OnePassDataIndexer indexer = new OnePassDataIndexer(es, 0);
            String[] predLabels = indexer.getPredLabels();
            predMap = new Object2IntOpenHashMap<String>();
            for (int i = 0; i < predLabels.length; i++) {
                predMap.put(predLabels[i], i);
            }
            outcomeLabels = indexer.getOutcomeLabels();
            outcomeMap = new Object2IntOpenHashMap<String>();
            for (int i = 0; i < outcomeLabels.length; i++) {
                outcomeMap.put(outcomeLabels[i], i);
            }

            // Adapt to ClearNLP Adagrad
            List<Instance> instanceList = new ArrayList<Instance>();
            es = new BasicEventStream(new PlainTextByLineDataStream(datafr));
            while (es.hasNext()) {
                Event event = es.next();
//                SparseVector sv = new SparseVector();
//                String[] context = event.getContext();
//                for (int i = 0; i < context.length; i++) {
//                    sv.add(predMap.get(context[i]));
//                }
//                Instance instance = new Instance(outcomeMap.get(event.getOutcome()), sv);
                Instance instance = instanceFromEvent(event);
                instanceList.add(instance);
            }

            // train MaxEnt model
            w = new WeightVector();
            AdaGrad optimizer = new AdaGrad(w, 0.001F, 0.0F);
            for (int i = 0; i < iterations; i++) {
                Collections.shuffle(instanceList);
                for (Instance instance : instanceList) {
                    optimizer.train(instance);
                }
            }

        } catch (Exception e) {
            System.out.print("Unable to create model due to exception: ");
            System.out.println(e);
        }
    }

    public Instance instanceFromEvent(Event event) {
        SparseVector sv = vectorFromContext(event.getContext());
        Instance instance = new Instance(outcomeMap.get(event.getOutcome()), sv);
        return instance;
    }

    private SparseVector vectorFromContext(String[] contexts) {
        SparseVector sv = new SparseVector();
        for (String context : contexts) {
            sv.add(predMap.get(context));
        }
        return sv;
    }

    public void saveModel() {
        if (modelFileName == null) {
            System.out.println("MaxEntModel.saveModel:  no modelFileName specified");
        } else {
            saveModel(modelFileName);
        }
    }

    public void saveModel(String modelFileName)  {
        try {
            FileOutputStream fos = new FileOutputStream(modelFileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(predMap);
            oos.writeObject(outcomeMap);
            oos.writeObject(outcomeLabels);
            oos.writeObject(w);
            oos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveModel(BufferedWriter writer) {
        System.err.println("Save to BufferedWriter is not supported in LogisticRegressionModel");
    }

    public void loadModel() {
        if (modelFileName == null) {
            System.err.println("MaxEntModel.loadModel:  no modelFileName specified");
        } else {
            loadModel(modelFileName);
        }
    }

    public void loadModel(String modelFileName) {
        try {
            FileInputStream fis = new FileInputStream(modelFileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            predMap = (Object2IntMap<String>)ois.readObject();
            outcomeMap = (Object2IntMap<String>)ois.readObject();
            outcomeLabels = (String[])ois.readObject();
            w = (WeightVector) ois.readObject();
        } catch (Exception e) {
            System.err.println("Error loading LogisticRegressionModel");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void loadModel(BufferedReader reader) {
        System.err.println("Save to BufferedWriter is not supported in LogisticRegressionModel");
    }


    public boolean isLoaded() {
        return w != null;
    }

    /**
     * (for a trained model) returns the probability that the Datum
     * <CODE>d</CODE> is classified as <CODE>value</CODE>.
     */

    public double prob(Datum d, String value) {
        String[] contexts = d.toArray();
        float[] scores = w.scores(new FeatureVector(vectorFromContext(contexts)));
        // return model.eval(d.toArray())[model.getIndex(value)];
        return scores[outcomeMap.get(value)];
    }

    /**
     * (for a trained model) returns the most likely outcome for Datum
     * <CODE>d</CODE>.
     */

    public String bestOutcome(Datum d) {
        String[] contexts = d.toArray();
        float[]  scores   = w.scores(new FeatureVector(vectorFromContext(contexts)));
        float bestScore = scores[0];
        int   bestIndex = 0;
        for (int i = 1; i < getNumOutcomes(); i++) {
            if (scores[i] > bestScore) {
                bestIndex = i;
                bestScore = scores[i];
            }
        }
        return outcomeLabels[bestIndex];
    }

    public int getNumOutcomes() {
        return outcomeMap.size();
    }

    public String getOutcome(int i) {
        return outcomeLabels[i];
    }

    public double[] getOutcomeProbabilities(Datum d) {
        String[] contexts = d.toArray();
        float[]  scores   = w.scores(new FeatureVector(vectorFromContext(contexts)));
        double[] result   = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            result[i] = scores[i];
        }
        return result;
    }


}
