package nn;

public class MeanSquaredError implements LossFunction {

    public double[] gradient(double[] predictions, double[] labels) {
        // Fehlergradient: Ableitung der Fehlerfunktion nach der Knotenausgabe
        // d.h. wie stark sich der Verlust ändert, wenn die Ausgabe des Knotens minimal verändert wird
        double[] grad = new double[labels.length];
        for (int i = 0; i < labels.length; i++) {
            grad[i] = (predictions[i] - labels[i]) / labels.length;
        }
        return grad;
    }

    public double loss(double[] predictions, double[] labels) {
        double sum = 0.0;
        for (int i = 0; i < labels.length; i++) {
            double diff = predictions[i] - labels[i];
            sum += 0.5 * (diff * diff);
        }
        return sum / labels.length;
    }
}
