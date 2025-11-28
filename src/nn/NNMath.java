package nn;

import nn.activation.ActivationFunction;

import java.util.Random;

public class NNMath {


    public static void main(String[] args) {
        int n = 8;  // Originalbild 8x8
        int m = 4;  // Downsample 4x4
        int k = 3;  // Fenstergröße 3x3

        // Originalbild: Quadrat in der Mitte
        double[] input = {
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 1, 1, 1, 1, 1, 1, 0,
                0, 1, 1, 1, 1, 1, 1, 0,
                0, 1, 1, 1, 1, 1, 1, 0,
                0, 1, 1, 1, 1, 1, 1, 0,
                0, 1, 1, 1, 1, 1, 1, 0,
                0, 1, 1, 1, 1, 1, 1, 0,
                0, 0, 0, 0, 0, 0, 0, 0
        };

        System.out.println("Originalbild:");
        printImage(input);
        System.out.println();

        double[] slideAvg = downSample_slideAvg(input, m, 3);
        double[] blockAvg = downSample_blockAvg(input, m);
        double[] blockMax = downSample_blockMax(input, m);


        System.out.println("\nDownsampled Bild: (slideAvg " + k + ")");
        printImage(roundDownSample(slideAvg));
        System.out.println();

        System.out.println("\nDownsampled Bild: (blockAvg)");
        printImage(roundDownSample(blockAvg));
        System.out.println();

        System.out.println("\nDownsampled Bild: (blockMax)");
        printImage(roundDownSample(blockMax));
        System.out.println();


    }

    public static void printImage(double[] input) {
        int n = (int) Math.sqrt(input.length);
        if (n * n != input.length) {
            throw new IllegalArgumentException("Input ist kein quadratisches Bild!");
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(String.format("%.2f ", input[i * n + j]));
            }
            System.out.println();
        }
    }

    public static double[] roundDownSample(double[] downSample) {
        double[] rounded = new double[downSample.length];
        for (int i = 0; i < downSample.length; i++) {
            rounded[i] = (downSample[i] >= 0.5) ? 1.0 : 0.0;
        }
        return rounded;
    }


    public static double[] downSample_slideAvg(double[] input, int m, int k) {
        // n = Originalbildgröße (n x n)
        // m = Größe des downSample (m x m)
        // k = Fenstergröße für Mittelwert
        //k > n/m furer sinnvolles Downsampleing!!!
        int n = (int) Math.sqrt(input.length);
        if (n * n != input.length) {
            throw new IllegalArgumentException("Input ist kein quadratisches Bild!");
        }
        double[] downSample = new double[m * m];
        int blockSize = k;
        // Berechne Schrittgröße für gleichmäßige Verteilung der Fenster über das Bild
        double step = (double) (n - blockSize) / (m - 1);

        for (int i = 0; i < m; i++) {           // Zeilen des downSample
            for (int j = 0; j < m; j++) {       // Spalten des downSample
                double sum = 0.0;
                int count = 0;
                // Berechne Startposition im Input
                int startRow = (int) Math.round(i * step);
                int startCol = (int) Math.round(j * step);
                // Schleife über k x k Fenster
                for (int wi = 0; wi < blockSize; wi++) {
                    for (int wj = 0; wj < blockSize; wj++) {
                        int row = startRow + wi;
                        int col = startCol + wj;
                        if (row < n && col < n) {  // innerhalb der Bildgrenzen
                            sum += input[row * n + col];
                            count++;
                        }
                    }
                }
                // Mittelwert
                downSample[i * m + j] = sum / count;
            }
        }

        return downSample;
    }

    public static double[] downSample_blockAvg(double[] input, int m) {
        int n = (int) Math.sqrt(input.length);
        if (n * n != input.length) {
            throw new IllegalArgumentException("Input ist kein quadratisches Bild!");
        }
        double[] downSample = new double[m * m];
        int blockSize = n / m;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                double sum = 0.0;
                int count = 0;
                for (int bi = 0; bi < blockSize; bi++) {
                    for (int bj = 0; bj < blockSize; bj++) {
                        int row = i * blockSize + bi;
                        int col = j * blockSize + bj;
                        sum += input[row * n + col];
                        count++;
                    }
                }
                downSample[i * m + j] = sum / count;
            }
        }
        return downSample;
    }

    public static double[] downSample_blockMax(double[] input, int m) {
        int n = (int) Math.sqrt(input.length);
        if (n * n != input.length) {
            throw new IllegalArgumentException("Input ist kein quadratisches Bild!");
        }
        double[] downSample = new double[m * m];
        int blockSize = n / m;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                double maxVal = 0.0;
                for (int bi = 0; bi < blockSize; bi++) {
                    for (int bj = 0; bj < blockSize; bj++) {
                        int row = i * blockSize + bi;
                        int col = j * blockSize + bj;
                        if (input[row * n + col] > maxVal) {
                            maxVal = input[row * n + col];
                        }
                    }
                }
                downSample[i * m + j] = maxVal;
            }
        }
        return downSample;
    }


    // Skalarprodukt berechnen
    public static double skalarProdukt(double[] w, double[] input) {
        double result = 0.0;
        for (int i = 0; i < w.length; i++) {
            result += w[i] * input[i];
        }
        return result;
    }

    public static int[] generatePermutation(int n, Random randG) {
        int[] result = new int[n];
        int[] pool = new int[n];

        // Hilfsarray initialisieren
        for (int i = 0; i < n; i++) {
            pool[i] = i;
        }

        // Fisher-Yates-Variante von hinten nach vorne
        for (int i = n - 1; i >= 0; i--) {
            int index = randG.nextInt(i + 1);
            result[i] = pool[index];
            pool[index] = pool[i]; // Ersetze die gewählte Zahl mit der letzten gültigen
        }

        int sum = 0;
        for (int i = 0; i < result.length; i++)
            sum += result[i];
        if (sum != n * (n - 1) / 2)
            System.out.println("Kacke");

        return result;
    }


    // Skalarprodukt
    public static double matVecDot(double[] w, double[] x) {
        double sum = 0.0;
        for (int i = 0; i < w.length; i++) sum += w[i] * x[i];
        return sum;
    }

    public static int argmax(double[] z) {
        int best = 0;
        for (int i = 1; i < z.length; i++) {
            if (z[i] > z[best]) best = i;
        }
        return best;
    }
//    // Gradient CrossEntropy + Softmax (vereinfachte Form)
//    public static double[] crossEntropyGradient(double[] logits, double[] target) {
//        double[] grad = new double[target.length];
//        double[] probs = softmax(logits);
//        for (int i = 0; i < target.length; i++) grad[i] = probs[i] - target[i];
//        return grad;
//    }
//
//    // Softmax
//    public static double[] softmax(double[] z) {
//        double max = z[0];
//        for (double v : z) if (v > max) max = v;
//        double sum = 0.0;
//        double[] out = new double[z.length];
//        for (int i = 0; i < z.length; i++) {
//            out[i] = Math.exp(z[i] - max);
//            sum += out[i];
//        }
//        for (int i = 0; i < z.length; i++) out[i] /= sum;
//        return out;
//    }


    /**
     * Softmax für ein Array von Logits
     *
     * @param z double[]: Output des Layers (Logits)
     * @return double[]: Softmax-Wahrscheinlichkeiten
     */
    public static double[] softmax(double[] z) {
        double max = z[0];
        for (int i = 1; i < z.length; i++) {
            if (z[i] > max) max = z[i];
        }

        double sum = 0.0;
        double[] result = new double[z.length];
        for (int i = 0; i < z.length; i++) {
            result[i] = Math.exp(z[i] - max); // numerisch stabil
            sum += result[i];
        }
        for (int i = 0; i < z.length; i++) {
            result[i] /= sum;
        }
        return result;
    }

    /**
     * Overload: Aktivierung auf Array anwenden
     */
    public static double[] activate(double[] z, ActivationFunction activation) {
        // this does not work for softmax!
        double[] result = new double[z.length];
        for (int i = 0; i < z.length; i++) {
            result[i] = activation.activate(z[i]);
        }
        return result;
    }
}
