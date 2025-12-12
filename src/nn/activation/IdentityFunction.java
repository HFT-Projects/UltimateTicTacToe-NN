package nn.activation;

public class IdentityFunction extends ActivationFunction {
    @Override
    public double activate(double x) {
        return x;
    }

    @Override
    public double activateDerivative(double x) {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IdentityFunction;
    }
}
