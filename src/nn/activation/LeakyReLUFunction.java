package nn.activation;

public class LeakyReLUFunction extends ActivationFunction {
    private final double alpha = 0.01d;

    @Override
    public double activate(double x) {
        return x < 0 ? alpha * x : x;
    }

    @Override
    public double activateDerivative(double x) {
        return x < 0 ? alpha : 1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LeakyReLUFunction;
    }
}
