package nn.loss;

public abstract class LossFunction {
    // Fehlergradient "gradient()": Ableitung der Loss-Funktion nach der Knotenausgabe (dL/da)
    // Beschreibt, wie stark sich der Verlust ändert, wenn die Ausgabe des Knotens minimal verändert wird

    // Hinweis:
    // - Bei linearen Ausgängen entspricht der Fehlergradient direkt dem Delta, das für Backpropagation verwendet wird
    // - Bei nichtlinearen Aktivierungen muss im Backward-Pass Delta = Fehlergradient * Aktivierungsableitung berechnet werden
    // - Für Softmax + CrossEntropy liefert der Fehlergradient direkt die Werte, die als Delta in Backpropagation genutzt werden
    public abstract double[] gradient(double[] prediction, double[] target);

    public abstract double loss(double[] predictions, double[] labels);

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        return 0;
    }
}
