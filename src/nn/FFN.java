package nn;

import nn.activation.ActivationFunction;
import nn.loss.LossFunction;
import nn.loss.MeanSquaredError;

import java.util.Arrays;
import java.util.Random;


public class FFN {
    private static final long SEED = 42L;
    private static final Random rand = new Random();

    static {
        rand.setSeed(SEED);
    }

    private int miniBatchCounter = 0;
    private final int miniBatchSize;   // wird beim Konstruktor gesetzt
    private final double[][][] gradW;  // W-Gradienten summiert
    private final double[][] gradB;    // Bias-Gradienten summiert


    private final int numLayers;
    private final int[] layerSizes;

    private final double[][][] W; // Gewichte: W[l][j][i]
    private final double[][] b; // Bias-Vektoren: b[l][j]
    private final double[][] a; // Aktivierungen: a[l][i]
    private final double[][] z; // Nettoeingänge: z[l][i]
    private final double[][] delta;

    private final ActivationFunction hiddenActivation;
    private final ActivationFunction outputActivation;

    public FFN(int[] layerSizes, ActivationFunction hiddenActivation, ActivationFunction outputActivation, int miniBatchSize) {
        this.layerSizes = layerSizes;
        this.numLayers = layerSizes.length;

        this.hiddenActivation = hiddenActivation;
        this.outputActivation = outputActivation;
        this.miniBatchSize = miniBatchSize;


        W = new double[numLayers][][];
        b = new double[numLayers][];
        a = new double[numLayers][];
        z = new double[numLayers][];
        delta = new double[numLayers][];
        gradW = new double[numLayers][][];
        gradB = new double[numLayers][];

        // Initialisierung
        for (int l = 1; l < numLayers; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            W[l] = new double[nOut][nIn];
            b[l] = new double[nOut];
            a[l] = new double[nOut];
            z[l] = new double[nOut];
            delta[l] = new double[nOut];
            gradW[l] = new double[nOut][nIn];
            gradB[l] = new double[nOut];
        }

        W[0] = null;
        b[0] = null;
        a[0] = new double[layerSizes[0]];
        z[0] = null;
        delta[0] = null;

        initWeights();

    }

    private void initWeights() {
        for (int l = 1; l < numLayers; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            for (int j = 0; j < nOut; j++) {
                b[l][j] = (rand.nextDouble() - 0.5);
                //neu: 0.0
                for (int i = 0; i < nIn; i++) {
                    W[l][j][i] = (rand.nextDouble() - 0.5);                        //neu: (rand.nextDouble() - 0.5) * 0.1;
//					W[l][j][i] = -1 + 2 * rand.nextDouble();

                }
            }
        }
        // System.out.println("Gewichte zufaellig initialisiert");
    }

    // ============================================================
    // FORWARD PASS (EINZELINPUT)
    // ============================================================
    public double[] forward(double[] input) {
        a[0] = input.clone();

        for (int l = 1; l < numLayers; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                double sum = b[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    sum += W[l][j][i] * a[l - 1][i];
                }
                z[l][j] = sum;
                if (l < numLayers - 1)
                    a[l][j] = hiddenActivation.activate(z[l][j]);
                else
                    a[l][j] = outputActivation.activate(z[l][j]);
            }
        }
        return a[numLayers - 1];
    }

    // ============================================================
    // BACKWARD PASS
    // ============================================================
    public void backward(double[] yTrue, LossFunction lossFunction) {
        int L = numLayers - 1;

        double[] gradOut = lossFunction.gradient(a[L], yTrue); // dL/da

        for (int j = 0; j < a[L].length; j++) {
            if (lossFunction instanceof MeanSquaredError) {
                delta[L][j] = gradOut[j] * outputActivation.activateDerivative(z[L][j]);
            } else {
                System.out.println("keine update moeglich");
            }
        }


        // Delta für Hidden-Schichten
        for (int l = L - 1; l > 0; l--) {
            for (int j = 0; j < layerSizes[l]; j++) {
                double sum = 0.0;
                for (int k = 0; k < layerSizes[l + 1]; k++) {
                    sum += W[l + 1][k][j] * delta[l + 1][k];
                }
                delta[l][j] = sum * hiddenActivation.activateDerivative(z[l][j]);
            }
        }
    }


    public void updateWeights(double learningRate) {
        // Gewichte und Bias updaten
        for (int l = 1; l < numLayers; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                b[l][j] -= learningRate * delta[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    W[l][j][i] -= learningRate * delta[l][j] * a[l - 1][i];
                }
            }
        }
    }


    public void trainFromAction(double[] state, double[] target, double learningRate, LossFunction lossFunction) {
        double[] predBefore = forward(state); // forward setzt intern a[][] und z[][]

        double[] predBeforeCopy = predBefore.clone();

        double lossBefore = lossFunction.loss(predBeforeCopy, target);

        backward(target, lossFunction);

        updateWeights(learningRate);

        double[] predAfter = forward(state); // vor/nach Update: hier bewusst nach dem Update
        double lossAfter = lossFunction.loss(predAfter, target);
//		    if(epoche%500==0)
//		    System.out.println("Epoche: " + epoche + " LossBefore: " + lossBefore + " LossAfter: " + lossAfter);
    }


    public double[] predictQ(double[] state) {
        int outputLayer = numLayers - 1;
        double[] Q = new double[a[outputLayer].length];

        forward(state);

        System.arraycopy(a[outputLayer], 0, Q, 0, Q.length);
        return Q;
    }

    // ===========================
    // Mini-Batch Training
    // ===========================
    public void trainMiniBatchFromAction(double[] state, double[] target, double learningRate, LossFunction lossFunction, int epoche) {
        // 1. Forward
        forward(state);

        // 2. Backward
        backward(target, lossFunction);

        // 3. Gradienten aufsummieren
        for (int l = 1; l < numLayers; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                gradB[l][j] += delta[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    gradW[l][j][i] += delta[l][j] * a[l - 1][i];
                }
            }
        }

        miniBatchCounter++;

        // 4. Prüfen, ob Batch voll ist
        if (miniBatchCounter >= miniBatchSize) {
            updateWeightsMiniBatch(learningRate);
            resetGradients();
            miniBatchCounter = 0;
        }
    }

    // ===========================
    // Gewichte nach Batch update
    // ===========================
    private void updateWeightsMiniBatch(double learningRate) {
        for (int l = 1; l < numLayers; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                b[l][j] -= learningRate * gradB[l][j] / miniBatchSize;
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    W[l][j][i] -= learningRate * gradW[l][j][i] / miniBatchSize;
                }
            }
        }
    }

    private void resetGradients() {
        for (int l = 1; l < numLayers; l++) {
            Arrays.fill(gradB[l], 0.0);
            for (int j = 0; j < layerSizes[l]; j++) {
                Arrays.fill(gradW[l][j], 0.0);
            }
        }
    }
}
