package nn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.Utils;
import nn.activation.ActivationFunction;
import nn.initialization.BiasInitializer;
import nn.initialization.BiasInitializerRandom;
import nn.initialization.WeightInitializer;
import nn.initialization.WeightInitializerRandom;
import nn.loss.LossFunction;
import nn.trainer.FFNTrainer;
import nn.trainer.FFNTrainerBGD;

import java.io.UncheckedIOException;
import java.util.*;


public class FFN {
    public final ActivationFunction hiddenActivation;
    public final ActivationFunction outputActivation;
    public final LossFunction lossFunction;
    public final BiasInitializer biasInitializer;
    public final WeightInitializer weightInitializer;

    public final int[] layerSizes;
    private final double[][][] W; // weights: W[l][j][i]
    private final double[][] b; // bias: b[l][j]

    // temp local variables for forward and backward pass
    // only because we want to avoid re-allocating memory on each pass
    // DO NOT READ BACK VALUES FROM THESE ARRAYS
    private final double[][] a; // activations: a[l][j]
    private final double[][] z; // pre-activations: z[l][j]
    private final double[][] delta; // deltas: delta[l][j]

    @SuppressWarnings("unused")
    public FFN(int[] layerSizes, ActivationFunction hiddenActivation, ActivationFunction outputActivation,
               LossFunction lossFunction) {
        this(layerSizes, hiddenActivation, outputActivation, lossFunction, new BiasInitializerRandom(),
                new WeightInitializerRandom());
    }

    public FFN(int[] layerSizes, ActivationFunction hiddenActivation, ActivationFunction outputActivation,
               LossFunction lossFunction, BiasInitializer biasInitializer, WeightInitializer weightInitializer) {
        this.layerSizes = layerSizes.clone();

        this.hiddenActivation = hiddenActivation;
        this.outputActivation = outputActivation;
        this.lossFunction = lossFunction;
        this.biasInitializer = biasInitializer;
        this.weightInitializer = weightInitializer;

        W = weightInitializer.initializeWeights(layerSizes);
        b = biasInitializer.initializeBias(layerSizes);

        a = new double[layerSizes.length][];
        z = new double[layerSizes.length][];
        for (int l = 1; l < layerSizes.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                a[l] = new double[layerSizes[l]];
                z[l] = new double[layerSizes[l]];
            }
        }

        delta = new double[layerSizes.length][];
        for (int i = 1; i < layerSizes.length; i++) {
            delta[i] = new double[layerSizes[i]];
        }
    }

    // ============================================================
    // FORWARD PASS
    // ============================================================
    private record ForwardReturn(double[][] a, double[][] z) {
    }

    private ForwardReturn forward(double[] input) {
        a[0] = input.clone();

        for (int l = 1; l < layerSizes.length; l++) {
            for (int j = 0; j < layerSizes[l]; j++) {
                double sum = b[l][j];
                for (int i = 0; i < layerSizes[l - 1]; i++) {
                    sum += W[l][j][i] * a[l - 1][i];
                }

                z[l][j] = sum;
                if (l < layerSizes.length - 1)
                    a[l][j] = hiddenActivation.activate(z[l][j]);
                else
                    a[l][j] = outputActivation.activate(z[l][j]);
            }
        }
        return new ForwardReturn(a, z);
    }

    // ============================================================
    // BACKWARD PASS
    // ============================================================
    private double[][] backward(double[] yTrue, double[][] a, double[][] z) {
        int last = layerSizes.length - 1;

        double[] gradOut = lossFunction.gradient(a[last], yTrue);

        // deltas for output layer
        for (int j = 0; j < a[last].length; j++) {
            delta[last][j] = gradOut[j] * outputActivation.activateDerivative(z[last][j]);
        }

        // deltas for hidden layers
        for (int l = last - 1; l > 0; l--) {
            for (int j = 0; j < layerSizes[l]; j++) {
                double sum = 0.0;
                for (int k = 0; k < layerSizes[l + 1]; k++) {
                    sum += W[l + 1][k][j] * delta[l + 1][k];
                }
                delta[l][j] = sum * hiddenActivation.activateDerivative(z[l][j]);
            }
        }

        return delta;
    }

    public double[] predictQ(double[] state) {
        int outputLayer = layerSizes.length - 1;

        double[][] a = forward(state).a();

        return a[outputLayer].clone();
    }

    public void train(FFNTrainer trainer, double[] state, double[] target, double learningRate) {
        ForwardReturn fwd = forward(state);
        double[][] delta = backward(target, fwd.a(), fwd.z());
        trainer.train(fwd.a(), fwd.z(), delta, b, W, layerSizes, learningRate);
    }

    public void applyTraining(FFNTrainerBGD trainer, double learningRate) {
        trainer.applyTraining(b, W, learningRate);
    }

    public void save(String filepath) {
        Map<String, Object> jsonO = new LinkedHashMap<>();
        jsonO.put("W", W);
        jsonO.put("b", b);
        jsonO.put("layerSizes", layerSizes);
        jsonO.put("hiddenActivation", Utils.activationToName.get(hiddenActivation));
        jsonO.put("outputActivation", Utils.activationToName.get(outputActivation));
        jsonO.put("lossFunction", Utils.lossToName.get(lossFunction));
        jsonO.put("biasInitializer", Utils.biasInitializerToName.get(biasInitializer));
        jsonO.put("weightInitializer", Utils.weightInitializerToName.get(weightInitializer));

        try {
            String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonO);

            try (var writer = new java.io.FileWriter(filepath)) {
                writer.write(json);
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static FFN load(String filepath) {
        try {
            String json;
            try (var reader = new java.io.FileReader(filepath)) {
                json = reader.readAllAsString();
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> map = new ObjectMapper().readValue(json, Map.class);

            @SuppressWarnings("unchecked")
            double[][][] Wwithout0 = ((List<List<List<Double>>>) map.get("W")).stream().skip(1)
                    .map(l2 -> l2.stream()
                            .map(l1 -> l1.stream().mapToDouble(Double::doubleValue).toArray())
                            .toArray(double[][]::new))
                    .toArray(double[][][]::new);

            double[][][] W = new double[Wwithout0.length + 1][][];
            W[0] = null;
            System.arraycopy(Wwithout0, 0, W, 1, Wwithout0.length);


            @SuppressWarnings("unchecked")
            double[][] bwithout0 = ((List<List<Double>>) map.get("b")).stream().skip(1)
                    .map(l1 -> l1.stream().mapToDouble(Double::doubleValue).toArray())
                    .toArray(double[][]::new);

            double[][] b = new double[bwithout0.length + 1][];
            b[0] = null;
            System.arraycopy(bwithout0, 0, b, 1, bwithout0.length);

            @SuppressWarnings("unchecked")
            int[] layerSizes = ((List<Integer>) map.get("layerSizes")).stream().mapToInt(Integer::intValue).toArray();

            ActivationFunction hiddenActivation = Utils.nameToActivation.get((String) map.get("hiddenActivation"));
            ActivationFunction outputActivation = Utils.nameToActivation.get((String) map.get("outputActivation"));
            LossFunction lossFunction = Utils.nameToLoss.get((String) map.get("lossFunction"));

            BiasInitializer biasInitializer = map.containsKey("biasInitializer") ? Utils.nameToBiasInitializer.get((String) map.get("biasInitializer")) : new BiasInitializerRandom();
            WeightInitializer weightInitializer = map.containsKey("weightInitializer") ? Utils.nameToWeightInitializer.get((String) map.get("weightInitializer")) : new WeightInitializerRandom();

            FFN ffn = new FFN(layerSizes, hiddenActivation, outputActivation, lossFunction, biasInitializer, weightInitializer);
            System.arraycopy(W, 0, ffn.W, 0, W.length);
            System.arraycopy(b, 0, ffn.b, 0, b.length);

            return ffn;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FFN other &&
                Arrays.deepEquals(this.W, other.W) &&
                Arrays.deepEquals(this.b, other.b) &&
                Arrays.equals(this.layerSizes, other.layerSizes) &&
                this.hiddenActivation.equals(other.hiddenActivation) &&
                this.outputActivation.equals(other.outputActivation) &&
                this.lossFunction.equals(other.lossFunction);
    }
}
