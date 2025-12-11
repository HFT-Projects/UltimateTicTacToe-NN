package nn;

import nn.activation.ActivationFunction;
import nn.loss.LossFunction;

import java.util.Arrays;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class FFN_MBGD extends FFN {
    private final int miniBatchSize;
    private int miniBatchCounter = 0;

    private final double[][][] gradW;   // sum of weight-gradients
    private final double[][] gradB;    // sum of bias-gradients

    public FFN_MBGD(int[] layerSizes, ActivationFunction hiddenActivation, ActivationFunction outputActivation, int miniBatchSize) {
        super(layerSizes, hiddenActivation, outputActivation);
        this.miniBatchSize = miniBatchSize;

        gradW = new double[layerSizes.length][][];
        gradB = new double[layerSizes.length][];

        for (int l = 1; l < layerSizes.length; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            gradW[l] = new double[nOut][nIn];
            gradB[l] = new double[nOut];
        }
    }

    public void train(double[] state, double[] target, double learningRate, LossFunction lossFunction) {
        ForwardReturn fwd = forward(state);

        double[][] delta = backward(target, fwd.a(), fwd.z(), lossFunction);

        for (int l = 1; l < delta.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                gradB[l][j] += delta[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    gradW[l][j][i] += delta[l][j] * fwd.a()[l - 1][i];
                }
            }
        }

        miniBatchCounter++;

        // check if mini-batch is complete
        if (miniBatchCounter >= miniBatchSize) {
            updateWeights(learningRate);
            resetGradients();
            miniBatchCounter = 0;
        }
    }

    private void updateWeights(double learningRate) {
        for (int l = 1; l < gradB.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                b[l][j] -= learningRate * gradB[l][j] / miniBatchSize;
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    W[l][j][i] -= learningRate * gradW[l][j][i] / miniBatchSize;
                }
            }
        }
    }

    private void resetGradients() {
        for (int l = 1; l < gradB.length; l++) {
            Arrays.fill(gradB[l], 0.0);
            for (int j = 0; j < layerSizes[l]; j++) {
                Arrays.fill(gradW[l][j], 0.0);
            }
        }
    }
}
