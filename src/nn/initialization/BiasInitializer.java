package nn.initialization;

public abstract class BiasInitializer {
    public abstract double[][] initializeBias(int[] layerSizes);

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        return 0;
    }
}
