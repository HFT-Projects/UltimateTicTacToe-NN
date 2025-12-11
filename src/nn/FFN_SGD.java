package nn;

import nn.activation.ActivationFunction;
import nn.loss.LossFunction;

public class FFN_SGD extends FFN {
    public FFN_SGD(int[] layerSizes, ActivationFunction hiddenActivation, ActivationFunction outputActivation) {
        super(layerSizes, hiddenActivation, outputActivation);
    }

    public void train(double[] state, double[] target, double learningRate, LossFunction lossFunction) {
        ForwardReturn fwd = forward(state);

        double[][] delta = backward(target, fwd.a(), fwd.z(), lossFunction);

        updateWeights(delta, fwd.a(), learningRate);
    }

    private void updateWeights(double[][] delta, double[][] a, double learningRate) {
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
