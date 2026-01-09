package nn.initialization;

public abstract class WeightInitializer {
    public abstract double[][][] initializeWeights(int[] layerSizes);

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        return 0;
    }
}
