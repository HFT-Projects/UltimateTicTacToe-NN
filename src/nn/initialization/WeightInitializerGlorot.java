package nn.initialization;

import java.util.Random;

public class WeightInitializerGlorot extends WeightInitializer {
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
            double limit = Math.sqrt(6.0 / (nIn + nOut));
            for (int j = 0; j < nOut; j++) {
                for (int i = 0; i < nIn; i++) {
                    W[l][j][i] = (rand.nextDouble() * 2.0 - 1.0) * limit;
                }
            }
        }
        return W;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WeightInitializerGlorot;
    }
}
