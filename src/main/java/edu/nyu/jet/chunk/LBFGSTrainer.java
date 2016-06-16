package edu.nyu.jet.chunk;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import cc.mallet.optimize.Optimizer;
import cc.mallet.optimize.tests.TestOptimizable;
import opennlp.model.DataIndexer;
import opennlp.model.EvalParameters;
import opennlp.model.MutableContext;
import opennlp.model.Prior;
import opennlp.maxent.GISModel;

import java.util.Random;

/**
 * Train an L2 regularized MaxEnt model with the L-BFGS algorithm
 *
 * @author yhe
 * @version 1.0
 */
public class LBFGSTrainer extends GISTrainer {

    /**
     * The regularization factor
     */
    private double c = 1.0;

    /**
     * Specifies whether unseen context/outcome pairs should be estimated as occur very infrequently.
     */
    private boolean useSimpleSmoothing = false;
    /**
     * Specifies whether a slack parameter should be used in the model.
     */
    private boolean useSlackParameter = false;
    /** Specified whether parameter updates should prefer a distribution of parameters which
     * is gaussian.
     */
    private boolean useGaussianSmoothing = false;
    private double sigma = 2.0;

    // If we are using smoothing, this is used as the "number" of
    // times we want the trainer to imagine that it saw a feature that it
    // actually didn't see.  Defaulted to 0.1.
    private double _smoothingObservation = 0.1;

    private boolean printMessages = false;

    /** Number of unique events which occured in the event set. */
    private int numUniqueEvents;
    /** Number of predicates. */
    private int numPreds;
    /** Number of outcomes. */
    private int numOutcomes;

    /** Records the array of predicates seen in each event. */
    private int[][] contexts;

    /** The value associated with each context. If null then context values are assumes to be 1. */
    private float[][] values;

    /** List of outcomes for each event i, in context[i]. */
    private int[] outcomeList;

    /** Records the num of times an event has been seen for each event i, in context[i]. */
    private int[] numTimesEventsSeen;

    /** The number of times a predicate occured in the training data. */
    private int[] predicateCounts;

    private int cutoff;

    /** Stores the String names of the outcomes.  The GIS only tracks outcomes
     as ints, and so this array is needed to save the model to disk and
     thereby allow users to know what the outcome was in human
     understandable terms. */
    private String[] outcomeLabels;

    /** Stores the String names of the predicates. The GIS only tracks
     predicates as ints, and so this array is needed to save the model to
     disk and thereby allow users to know what the outcome was in human
     understandable terms. */
    private String[] predLabels;

    /** Stores the observed expected values of the features based on training data. */
    private MutableContext[] observedExpects;

    /** Stores the estimated parameter value of each predicate during iteration */
    private MutableContext[] params;

    /** Stores the expected values of the features based on the current models */
    private MutableContext[] modelExpects;

    /** This is the prior distribution that the model uses for training. */
    private Prior prior;


    /** Observed expectation of correction feature. */
    private double cfObservedExpect;
    /** A global variable for the models expected value of the correction feature. */
    private double CFMOD;

    private final double NEAR_ZERO = 0.01;
    private final double LLThreshold = 0.0001;

    /** Stores the output of the current model on a single event during
     *  training.  This we be reset for every event for every iteration.  */
    //double[] modelDistribution;
    /** Stores the number of features that get fired per event. */
    //int[] numfeats;
    /** Initial probability for all outcomes. */

    //EvalParameters evalParams;

    /**
     * Create LBFGS trainer
     */
    public LBFGSTrainer() {
        super();
    }

    /**
     * Create LBFGS trainer with regularization parameter c
     */
    public LBFGSTrainer(double c) {
        super();
        this.c = c;
    }

    /**
     * Train a model using the GIS algorithm.
     *
     * @param iterations  The number of GIS iterations to perform.
     * @param di The data indexer used to compress events in memory.
     * @param modelPrior The prior distribution used to train this model.
     * @return The newly trained model, which can be used immediately or saved
     *         to disk using an opennlp.maxent.io.GISModelWriter object.
     */
    public GISModel trainModel(int iterations, DataIndexer di, Prior modelPrior, int cutoff) {
        /************** Incorporate all of the needed info ******************/
        display("Incorporating indexed data for training...  \n");
        contexts = di.getContexts();
        values = di.getValues();
        this.cutoff = cutoff;
        predicateCounts = di.getPredCounts();
        numTimesEventsSeen = di.getNumTimesEventsSeen();
        numUniqueEvents = contexts.length;
        this.prior = modelPrior;
        //printTable(contexts);

        // determine the correction constant and its inverse
        int correctionConstant = 1;
        for (int ci = 0; ci < contexts.length; ci++) {
            if (values == null || values[ci] == null) {
                if (contexts[ci].length > correctionConstant) {
                    correctionConstant = contexts[ci].length;
                }
            }
            else {
                float cl = values[ci][0];
                for (int vi=1;vi<values[ci].length;vi++) {
                    cl+=values[ci][vi];
                }

                if (cl > correctionConstant) {
                    correctionConstant=(int) Math.ceil(cl);
                }
            }
        }
        display("done.\n");

        outcomeLabels = di.getOutcomeLabels();
        outcomeList = di.getOutcomeList();
        numOutcomes = outcomeLabels.length;

        predLabels = di.getPredLabels();
        prior.setLabels(outcomeLabels,predLabels);
        numPreds = predLabels.length;

        display("\tNumber of Event Tokens: " + numUniqueEvents + "\n");
        display("\t    Number of Outcomes: " + numOutcomes + "\n");
        display("\t  Number of Predicates: " + numPreds + "\n");

        // set up feature arrays
        float[][] predCount = new float[numPreds][numOutcomes];
        for (int ti = 0; ti < numUniqueEvents; ti++) {
            for (int j = 0; j < contexts[ti].length; j++) {
                if (values != null && values[ti] != null) {
                    predCount[contexts[ti][j]][outcomeList[ti]] += numTimesEventsSeen[ti]*values[ti][j];
                }
                else {
                    predCount[contexts[ti][j]][outcomeList[ti]] += numTimesEventsSeen[ti];
                }
            }
        }

        //printTable(predCount);
        di = null; // don't need it anymore

        // A fake "observation" to cover features which are not detected in
        // the data.  The default is to assume that we observed "1/10th" of a
        // feature during training.
        final double smoothingObservation = _smoothingObservation;

        // Get the observed expectations of the features. Strictly speaking,
        // we should divide the counts by the number of Tokens, but because of
        // the way the model's expectations are approximated in the
        // implementation, this is cancelled out when we compute the next
        // iteration of a parameter, making the extra divisions wasteful.
        params = new MutableContext[numPreds];
        modelExpects = new MutableContext[numPreds];
        observedExpects = new MutableContext[numPreds];

        // The model does need the correction constant and the correction feature. The correction constant
        // is only needed during training, and the correction feature is not necessary.
        // For compatibility reasons the model contains form now on a correction constant of 1,
        // and a correction param 0.
        evalParams = new EvalParameters(params,0,1,numOutcomes);
        int[] activeOutcomes = new int[numOutcomes];
        int[] outcomePattern;
        int[] allOutcomesPattern= new int[numOutcomes];
        for (int oi = 0; oi < numOutcomes; oi++) {
            allOutcomesPattern[oi] = oi;
        }
        int numActiveOutcomes = 0;
        for (int pi = 0; pi < numPreds; pi++) {
            numActiveOutcomes = 0;
            if (useSimpleSmoothing) {
                numActiveOutcomes = numOutcomes;
                outcomePattern = allOutcomesPattern;
            }
            else { //determine active outcomes
                for (int oi = 0; oi < numOutcomes; oi++) {
                    if (predCount[pi][oi] > 0 && predicateCounts[pi] >= cutoff) {
                        activeOutcomes[numActiveOutcomes] = oi;
                        numActiveOutcomes++;
                    }
                }
                if (numActiveOutcomes == numOutcomes) {
                    outcomePattern = allOutcomesPattern;
                }
                else {
                    outcomePattern = new int[numActiveOutcomes];
                    for (int aoi=0;aoi<numActiveOutcomes;aoi++) {
                        outcomePattern[aoi] = activeOutcomes[aoi];
                    }
                }
            }
            params[pi] = new MutableContext(outcomePattern,new double[numActiveOutcomes]);
            modelExpects[pi] = new MutableContext(outcomePattern,new double[numActiveOutcomes]);
            observedExpects[pi] = new MutableContext(outcomePattern,new double[numActiveOutcomes]);
            for (int aoi=0;aoi<numActiveOutcomes;aoi++) {
                int oi = outcomePattern[aoi];
                params[pi].setParameter(aoi, 0.0);
                modelExpects[pi].setParameter(aoi, 0.0);
                if (predCount[pi][oi] > 0) {
                    observedExpects[pi].setParameter(aoi, predCount[pi][oi]);
                }
                else if (useSimpleSmoothing) {
                    observedExpects[pi].setParameter(aoi,smoothingObservation);
                }
            }
        }

        // compute the expected value of correction
        if (useSlackParameter) {
            int cfvalSum = 0;
            for (int ti = 0; ti < numUniqueEvents; ti++) {
                for (int j = 0; j < contexts[ti].length; j++) {
                    int pi = contexts[ti][j];
                    if (!modelExpects[pi].contains(outcomeList[ti])) {
                        cfvalSum += numTimesEventsSeen[ti];
                    }
                }
                cfvalSum += (correctionConstant - contexts[ti].length) * numTimesEventsSeen[ti];
            }
            if (cfvalSum == 0) {
                cfObservedExpect = Math.log(NEAR_ZERO); //nearly zero so log is defined
            }
            else {
                cfObservedExpect = Math.log(cfvalSum);
            }
        }
        predCount = null; // don't need it anymore

        display("...done.\n");

        modelDistribution = new double[numOutcomes];
        numfeats = new int[numOutcomes];

        /***************** Find the parameters ************************/
        display("Computing model parameters...\n");
        findParameters();

        /*************** Create and return the model ******************/
        // To be compatible with old models the correction constant is always 1
        return new GISModel(params, predLabels, outcomeLabels, 1, evalParams.getCorrectionParam());
    }


    /* Estimate and return the model parameters. */
    private void findParameters() {
        MaxEntOptimization optimizable = this.new MaxEntOptimization();
//        Random r = new Random();
//        System.err.println("Pass 1");
//        boolean valid =
//                TestOptimizable.testValueAndGradientRandomParameters(optimizable, r);
//
//        System.err.println("Pass 2");
//        valid = TestOptimizable.testValueAndGradientRandomParameters(optimizable, r);
//
//        System.err.println("Pass 3");
//        valid = TestOptimizable.testValueAndGradientRandomParameters(optimizable, r);
//
//        System.err.println("Pass 4");
//        valid = TestOptimizable.testValueAndGradientRandomParameters(optimizable, r);
//
//        System.err.println("Pass 5");
//        valid = TestOptimizable.testValueAndGradientRandomParameters(optimizable, r);
        Optimizer optimizer = new LimitedMemoryBFGS(optimizable);

        boolean converged = false;

        try {
            converged = optimizer.optimize();
        } catch (Exception e) {
            // This exception may be thrown if L-BFGS
            //  cannot step in the current direction.
            // This condition does not necessarily mean that
            //  the optimizer has failed, but it doesn't want
            //  to claim to have succeeded...
            e.printStackTrace();
        }

        if (converged) {
            System.out.println("L-BFGS converged.");
        }
        else {
            System.out.println("L-BFGS stopped before convergence.");
        }

        // kill a bunch of these big objects now that we don't need them
        observedExpects = null;
        modelExpects = null;
        numTimesEventsSeen = null;
        contexts = null;
    }

    private void display(String s) {
        if (printMessages)
            System.out.print(s);
    }

    class MaxEntOptimization implements Optimizable.ByGradientValue {

        int numOfParameters;

        int[][] indexMapping;

        public MaxEntOptimization() {
            numOfParameters = countParameterLength();
            buildIndexMapping();
        }

        private void buildIndexMapping() {
            indexMapping = new int[numOfParameters][2];
            int i = 0;
            for (int j = 0; j < params.length; j++) {
                double[] parameters = params[j].getParameters();
                for (int k = 0; k < parameters.length; k++) {
                    indexMapping[i][0] = j;
                    indexMapping[i][1] = k;
                    i++;
                }
            }
        }

        private double innerProduct(double[] a, double[] b) {
            double result = 0.0;
            for (int i = 0; i < a.length; i++) {
                result = a[i]*b[i];
            }
            return result;
        }

        private int countParameterLength() {
            int numOfParameters = 0;
            for (MutableContext param : params) {
                numOfParameters += param.getParameters().length;
            }
            return numOfParameters;
        }

        // The following get/set methods satisfy the Optimizable interface

        public double getValue() {

            double loglikelihood = 0;
            // Update with L2 regularization
            for (int pi = 0; pi < numPreds; pi++) {
                if (predicateCounts[pi] >= cutoff) {
                    double[] paramsForPi = params[pi].getParameters();
                    loglikelihood += innerProduct(paramsForPi, paramsForPi);
                }
            }
            loglikelihood = -c / 2 * loglikelihood;

            // eval mode and calculate log-likelihood
            for (int ei = 0; ei < numUniqueEvents; ei++) {
                if (values != null) {
                    prior.logPrior(modelDistribution,contexts[ei],values[ei]);
                    GISModel.eval(contexts[ei], values[ei], modelDistribution, evalParams);
                }
                else {
                    prior.logPrior(modelDistribution,contexts[ei]);
                    GISModel.eval(contexts[ei], modelDistribution, evalParams);
                }
                loglikelihood += Math.log(modelDistribution[outcomeList[ei]]) * numTimesEventsSeen[ei];
            }
            display(".");
            //System.err.println("loglikelihood: " + loglikelihood);
            return loglikelihood;
        }

        public void getValueGradient(double[] gradient) {
            int i = 0;
            for (MutableContext param : params) {
                double[] labelParameters = param.getParameters();
                for (int j = 0; j < labelParameters.length; j++) {
                    gradient[i] = -c * labelParameters[j];
                    i++;
                }
            }
            for (int ei = 0; ei < numUniqueEvents; ei++) {
                if (values != null) {
                    prior.logPrior(modelDistribution, contexts[ei], values[ei]);
                    GISModel.eval(contexts[ei], values[ei], modelDistribution, evalParams);
                } else {
                    prior.logPrior(modelDistribution, contexts[ei]);
                    GISModel.eval(contexts[ei], modelDistribution, evalParams);
                }
                for (int j = 0; j < contexts[ei].length; j++) {
                    int pi = contexts[ei][j];
                    if (predicateCounts[pi] >= cutoff) {
                        int[] activeOutcomes = modelExpects[pi].getOutcomes();
                        for (int aoi = 0; aoi < activeOutcomes.length; aoi++) {
                            int oi = activeOutcomes[aoi];
                            if (values != null && values[ei] != null) {
                                modelExpects[pi].updateParameter(aoi, modelDistribution[oi] * values[ei][j] * numTimesEventsSeen[ei]);
                            } else {
                                modelExpects[pi].updateParameter(aoi, modelDistribution[oi] * numTimesEventsSeen[ei]);
                            }
                        }
                    }
                }
            }

            i = 0;
            for (int pi = 0; pi < params.length; pi++) {
                double[] modelExpectsForParam = modelExpects[pi].getParameters();
                double[] observedExpectsForParam = observedExpects[pi].getParameters();
                if (modelExpectsForParam.length != observedExpectsForParam.length) {
                    System.err.println("Length of modelExpects and observedExpects should equal.");
                    return;
                }
                for (int j = 0; j < modelExpectsForParam.length; j++) {
                    gradient[i] = gradient[i] + observedExpectsForParam[j] - modelExpectsForParam[j];
                    i++;
                }
            }
            // Clean up model expects
            for (int j = 0; j < modelExpects.length; j++) {
                int piLen = modelExpects[j].getParameters().length;
                for (int k = 0; k < piLen; k++) {
                    modelExpects[j].setParameter(k, 0.0); // re-initialize to 0.0's
                }
            }

//          //  System.err.println("numOfParams:" + numOfParameters);
//          //  System.err.println("i:" + i + "\tgradient:");
//          //  for (int k = 0; k < gradient.length; k++) {
//          //      System.err.print(gradient[k] + " ");
//          //  }
//          //  System.err.println();
        }

        public int getNumParameters() {
            return numOfParameters;
        }

        public double getParameter(int i) {
            int j = indexMapping[i][0];
            int k = indexMapping[i][1];
            return params[j].getParameters()[k];
        }

        public void getParameters(double[] buffer) {
            int i = 0;
            for (MutableContext param : params) {
                double[] parameters = param.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    buffer[i] = parameters[j];
                    i++;
                }
            }
        }

        public void setParameter(int i, double r) {
            int j = indexMapping[i][0];
            int k = indexMapping[i][1];
            params[j].getParameters()[k] = r;
        }

        public void setParameters(double[] newParameters) {
            int i = 0;
            for (MutableContext param : params) {
                double[] parameters = param.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    parameters[j] = newParameters[i];
                    i++;
                }
            }
        }
    }


}
