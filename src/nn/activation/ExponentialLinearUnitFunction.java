package nn.activation;

public class ExponentialLinearUnitFunction implements ActivationFunction{
    private final double alpha = 1;

    @Override
    public double activate(double x) {
        return x >= 0 ? x : alpha * (Math.exp(x) - 1);
    }

    @Override
    public double activateDerivative(double x) {
        return x >= 0 ? 1 : activate(x) + alpha;
    }
}
