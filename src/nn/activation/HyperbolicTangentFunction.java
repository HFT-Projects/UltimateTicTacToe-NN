package nn.activation;

public class HyperbolicTangentFunction implements ActivationFunction{
    @Override
    public double activate(double x) {
        return Math.tanh(x);
    }

    @Override
    public double activateDerivative(double x) {
        double t = Math.tanh(x);
        return 1.0 - t * t;
    }
}
