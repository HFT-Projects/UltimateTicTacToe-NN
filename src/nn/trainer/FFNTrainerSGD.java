package nn.trainer;

public class FFNTrainerSGD implements FFNTrainer {
    public void train(double[][] a, double[][] z, double[][] delta, double[][] b, double[][][] W, int[] layerSizes, double learningRate) {
        updateWeights(delta, a, b, W, layerSizes, learningRate);
    }

    private void updateWeights(double[][] delta, double[][] a, double[][] b, double[][][] W, int[] layerSizes, double learningRate) {
        for (int l = 1; l < delta.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                b[l][j] -= learningRate * delta[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    W[l][j][i] -= learningRate * delta[l][j] * a[l - 1][i];
                }
            }
        }
    }
}
