package nn.activation;

public abstract class ActivationFunction {
    public abstract double activate(double x);

    public abstract double activateDerivative(double x);

    @Override
    public int hashCode() {
        return 0;
    }
}
