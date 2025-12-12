package nn.activation;

public class HyperbolicTangentFunction extends ActivationFunction {
    @Override
    public double activate(double x) {
        return Math.tanh(x);
    }

    @Override
    public double activateDerivative(double x) {
        double t = Math.tanh(x);
        return 1.0 - t * t;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HyperbolicTangentFunction;
    }
}
