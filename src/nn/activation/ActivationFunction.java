package nn.activation;

public interface ActivationFunction {
    double activate(double x);
    double activateDerivative(double x);
}
