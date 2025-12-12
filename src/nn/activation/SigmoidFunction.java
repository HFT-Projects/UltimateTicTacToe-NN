package nn.activation;

public class SigmoidFunction extends ActivationFunction {
    @Override
    public double activate(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    @Override
    public double activateDerivative(double x) {
        double s = 1.0 / (1.0 + Math.exp(-x));
        return s * (1.0 - s);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SigmoidFunction;
    }
}
