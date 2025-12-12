package nn.activation;

public class RectifiedLinearUnitFunction extends ActivationFunction {
    @Override
    public double activate(double x) {
        return Math.max(0.0, x);
    }

    @Override
    public double activateDerivative(double x) {
        return x > 0 ? 1.0 : 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RectifiedLinearUnitFunction;
    }
}
