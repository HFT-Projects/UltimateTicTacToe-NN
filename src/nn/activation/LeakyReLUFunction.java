package nn.activation;

public class LeakyReLUFunction implements ActivationFunction{
    private final double alpha = 0.01d;

    @Override
    public double activate(double x) {
        return x < 0 ? alpha * x : x;
    }

    @Override
    public double activateDerivative(double x) {
        return x < 0 ? alpha : 1;
    }
}
