package nn.initialization;

import java.util.Random;

public class BiasInitializerRandom extends BiasInitializer {
    Random rand = new Random();

    @Override
    public double[][] initializeBias(int[] layerSizes) {
        double[][] b = new double[layerSizes.length][];
        b[0] = null;

        for (int l = 1; l < layerSizes.length; l++) {
            int nOut = layerSizes[l];
            b[l] = new double[nOut];
        }

        for (int l = 1; l < layerSizes.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                b[l][j] = (rand.nextDouble() - 0.5);
            }
        }
        return b;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BiasInitializerRandom;
    }
}
