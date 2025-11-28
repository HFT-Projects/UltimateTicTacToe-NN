package nn.activation;

public class IdentityFunction implements ActivationFunction{
    @Override
    public double activate(double x) {
        return x;
    }

    @Override
    public double activateDerivative(double x) {
        return 1;
    }

}
