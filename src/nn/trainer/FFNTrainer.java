package nn.trainer;

public interface FFNTrainer {
    void train(double[][] a, double[][] delta, double[][] b, double[][][] W, int[] layerSizes, double learningRate);
}
