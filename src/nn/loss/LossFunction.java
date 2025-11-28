package nn.loss;

public interface LossFunction {
    // Fehlergradient "gradient()": Ableitung der Loss-Funktion nach der Knotenausgabe (dL/da)
    // Beschreibt, wie stark sich der Verlust ändert, wenn die Ausgabe des Knotens minimal verändert wird

    // Hinweis:
    // - Bei linearen Ausgängen entspricht der Fehlergradient direkt dem Delta, das für Backpropagation verwendet wird
    // - Bei nichtlinearen Aktivierungen muss im Backward-Pass Delta = Fehlergradient * Aktivierungsableitung berechnet werden
    // - Für Softmax + CrossEntropy liefert der Fehlergradient direkt die Werte, die als Delta in Backpropagation genutzt werden
    double[] gradient(double[] prediction, double[] target);

    double loss(double[] predictions, double[] labels);
}
