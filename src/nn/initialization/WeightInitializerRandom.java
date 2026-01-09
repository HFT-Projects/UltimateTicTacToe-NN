package nn.initialization;

import java.util.Random;

public class WeightInitializerRandom extends WeightInitializer {
    Random rand = new Random();

    @Override
    public double[][][] initializeWeights(int[] layerSizes) {
        double[][][] W = new double[layerSizes.length][][];
        W[0] = null;

        for (int l = 1; l < layerSizes.length; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            W[l] = new double[nOut][nIn];
        }

        for (int l = 1; l < layerSizes.length; l++) {
            int nIn = layerSizes[l - 1];
            int nOut = layerSizes[l];
            for (int j = 0; j < nOut; j++) {
                for (int i = 0; i < nIn; i++) {
                    W[l][j][i] = (rand.nextDouble() - 0.5);
                }
            }
        }
        return W;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WeightInitializerRandom;
    }
}
