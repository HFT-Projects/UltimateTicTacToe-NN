package nn.activation;

public class SwishFunction implements ActivationFunction{
    private static final ActivationFunction sigmoid = new SigmoidFunction();

    @Override
    public double activate(double x) {
        return x * sigmoid.activate(x);
    }

    @Override
    public double activateDerivative(double x) {
        return (x + Math.sinh(x)) / (4 * Math.pow(Math.cosh(x / 2), 2)) + 0.5;
    }
}
