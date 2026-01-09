package nn.initialization;

public class BiasInitializerNull extends BiasInitializer {
    @Override
    public double[][] initializeBias(int[] layerSizes) {
        double[][] b = new double[layerSizes.length][];
        b[0] = null;

        for (int l = 1; l < layerSizes.length; l++) {
            int nOut = layerSizes[l];
            b[l] = new double[nOut];
        }

        return b;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BiasInitializerNull;
    }
}
