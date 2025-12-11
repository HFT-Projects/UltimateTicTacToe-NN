package nn.trainer;

import java.util.Arrays;

@SuppressWarnings("unused")
public class FFNTrainerMBGB implements FFNTrainer {
    private final int miniBatchSize;
    private int miniBatchCounter = 0;

    private final double[][][] gradW;   // sum of weight-gradients
    private final double[][] gradB;    // sum of bias-gradients
    private final int[] layerSizes;

    public FFNTrainerMBGB(int[] layerSizes, int miniBatchSize) {
        this.miniBatchSize = miniBatchSize;
        this.layerSizes = layerSizes;

        gradW = new double[layerSizes.length][][];
        gradB = new double[layerSizes.length][];

        for (int l = 1; l < layerSizes.length; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            gradW[l] = new double[nOut][nIn];
            gradB[l] = new double[nOut];
        }
    }

    public void train(double[][] a, double[][] z, double[][] delta, double[][] b, double[][][] W, int[] layerSizes, double learningRate) {
        if (!Arrays.equals(this.layerSizes, layerSizes))
            throw new IllegalArgumentException("Layer sizes do not match trainer configuration.");

        for (int l = 1; l < delta.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                gradB[l][j] += delta[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    gradW[l][j][i] += delta[l][j] * a[l - 1][i];
                }
            }
        }

        miniBatchCounter++;

        // check if mini-batch is complete
        if (miniBatchCounter >= miniBatchSize) {
            updateWeights(learningRate, b, W, layerSizes);
            resetGradients(layerSizes);
            miniBatchCounter = 0;
        }
    }

    private void updateWeights(double learningRate, double[][] b, double[][][] W, int[] layerSizes) {
        for (int l = 1; l < gradB.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                b[l][j] -= learningRate * gradB[l][j] / miniBatchSize;
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    W[l][j][i] -= learningRate * gradW[l][j][i] / miniBatchSize;
                }
            }
        }
    }

    private void resetGradients(int[] layerSizes) {
        for (int l = 1; l < gradB.length; l++) {
            Arrays.fill(gradB[l], 0.0);
            for (int j = 0; j < layerSizes[l]; j++) {
                Arrays.fill(gradW[l][j], 0.0);
            }
        }
    }
}
