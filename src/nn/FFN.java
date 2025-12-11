package nn;

import nn.activation.ActivationFunction;
import nn.loss.LossFunction;
import nn.loss.MeanSquaredError;
import nn.trainer.FFNTrainer;

import java.util.Random;


public class FFN {
    private static final long SEED = 42L;
    private static final Random rand = new Random();

    static {
        rand.setSeed(SEED);
    }

    private final LossFunction lossFunction;

    private final double[][][] W; // weights: W[l][j][i]
    private final double[][] b; // bias: b[l][j]
    public final int[] layerSizes;

    private final double[][] a; // activations: a[l][j]
    private final double[][] z; // pre-activations: z[l][j]
    private final double[][] delta; // deltas: delta[l][j]

    private final ActivationFunction hiddenActivation;
    private final ActivationFunction outputActivation;

    public FFN(int[] layerSizes, ActivationFunction hiddenActivation, ActivationFunction outputActivation, LossFunction lossFunction) {
        this.layerSizes = layerSizes.clone();

        this.hiddenActivation = hiddenActivation;
        this.outputActivation = outputActivation;
        this.lossFunction = lossFunction;

        W = new double[layerSizes.length][][];
        b = new double[layerSizes.length][];

        W[0] = null;
        b[0] = null;

        for (int l = 1; l < layerSizes.length; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            W[l] = new double[nOut][nIn];
            b[l] = new double[nOut];
        }

        initWeights(layerSizes);

        a = new double[layerSizes.length][];
        z = new double[layerSizes.length][];
        for (int l = 1; l < layerSizes.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                a[l] = new double[layerSizes[l]];
                z[l] = new double[layerSizes[l]];
            }
        }

        delta = new double[layerSizes.length][];
        for (int i = 1; i < layerSizes.length; i++) {
            delta[i] = new double[layerSizes[i]];
        }
    }

    private void initWeights(int[] layerSizes) {
        for (int l = 1; l < layerSizes.length; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            for (int j = 0; j < nOut; j++) {
                b[l][j] = (rand.nextDouble() - 0.5);
                for (int i = 0; i < nIn; i++) {
                    W[l][j][i] = (rand.nextDouble() - 0.5);
                }
            }
        }
    }

    // ============================================================
    // FORWARD PASS
    // ============================================================
    private record ForwardReturn(double[][] a, double[][] z) {
    }

    private ForwardReturn forward(double[] input) {
        a[0] = input.clone();

        for (int l = 1; l < layerSizes.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                double sum = b[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    sum += W[l][j][i] * a[l - 1][i];
                }

                z[l][j] = sum;
                if (l < layerSizes.length - 1)
                    a[l][j] = hiddenActivation.activate(z[l][j]);
                else
                    a[l][j] = outputActivation.activate(z[l][j]);
            }
        }
        return new ForwardReturn(a, z);
    }

    // ============================================================
    // BACKWARD PASS
    // ============================================================
    private double[][] backward(double[] yTrue, double[][] a, double[][] z) {
        int last = layerSizes.length - 1;

        double[] gradOut = lossFunction.gradient(a[last], yTrue);

        // deltas for output layer
        for (int j = 0; j < a[last].length; j++) {
            if (lossFunction instanceof MeanSquaredError) {
                delta[last][j] = gradOut[j] * outputActivation.activateDerivative(z[last][j]);
            } else {
                throw new IllegalArgumentException("Only MeanSquaredError loss function is supported for output layer.");
            }
        }

        // deltas for hidden layers
        for (int l = last - 1; l > 0; l--) {
            for (int j = 0; j < layerSizes[l]; j++) {
                double sum = 0.0;
                for (int k = 0; k < layerSizes[l + 1]; k++) {
                    sum += W[l + 1][k][j] * delta[l + 1][k];
                }
                delta[l][j] = sum * hiddenActivation.activateDerivative(z[l][j]);
            }
        }

        return delta;
    }

    public double[] predictQ(double[] state) {
        int outputLayer = layerSizes.length - 1;

        double[][] a = forward(state).a();

        return a[outputLayer];
    }

    public void train(FFNTrainer trainer, double[] state, double[] target, double learningRate) {
        ForwardReturn fwd = forward(state);
        double[][] delta = backward(target, fwd.a(), fwd.z());
        trainer.train(fwd.a(), fwd.z(), delta, b, W, layerSizes, learningRate);
    }
}
