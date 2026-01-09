package helper;

import nn.activation.*;
import nn.initialization.*;
import nn.loss.*;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.GlobalBoard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class Utils {
    private Utils() {
    }

    public static final Map<ENDED_STATUS, PLAYER> ENDED_STATUS_TO_PLAYER;
    public static final Map<PLAYER, ENDED_STATUS> PLAYER_TO_ENDED_STATUS;

    public static final Map<ActivationFunction, String> activationToName;
    public static final Map<String, ActivationFunction> nameToActivation;

    public static final Map<LossFunction, String> lossToName;
    public static final Map<String, LossFunction> nameToLoss;

    public static final Map<BiasInitializer, String> biasInitializerToName;
    public static final Map<String, BiasInitializer> nameToBiasInitializer;

    public static final Map<WeightInitializer, String> weightInitializerToName;
    public static final Map<String, WeightInitializer> nameToWeightInitializer;

    static {
        // create unmodifiable maps for conversions

        Map<ENDED_STATUS, PLAYER> endedStatusToPlayer = new HashMap<>();
        endedStatusToPlayer.put(ENDED_STATUS.X, PLAYER.X);
        endedStatusToPlayer.put(ENDED_STATUS.O, PLAYER.O);
        endedStatusToPlayer.put(ENDED_STATUS.TIE, null);
        endedStatusToPlayer.put(null, null);
        ENDED_STATUS_TO_PLAYER = Collections.unmodifiableMap(endedStatusToPlayer);

        Map<PLAYER, ENDED_STATUS> playerToEndedStatus = new HashMap<>();
        playerToEndedStatus.put(PLAYER.X, ENDED_STATUS.X);
        playerToEndedStatus.put(PLAYER.O, ENDED_STATUS.O);
        playerToEndedStatus.put(null, null);
        PLAYER_TO_ENDED_STATUS = Collections.unmodifiableMap(playerToEndedStatus);


        Map<ActivationFunction, String> activationToNameTemp = new java.util.LinkedHashMap<>();
        activationToNameTemp.put(new ExponentialLinearUnitFunction(), "ELU");
        activationToNameTemp.put(new HyperbolicTangentFunction(), "Tanh");
        activationToNameTemp.put(new IdentityFunction(), "Identity");
        activationToNameTemp.put(new LeakyReLUFunction(), "LeakyReLU");
        activationToNameTemp.put(new RectifiedLinearUnitFunction(), "ReLU");
        activationToNameTemp.put(new SigmoidFunction(), "Sigmoid");
        activationToNameTemp.put(new SwishFunction(), "Swish");
        activationToName = Collections.unmodifiableMap(activationToNameTemp);

        Map<String, ActivationFunction> nameToActTemp = new java.util.LinkedHashMap<>();
        for (ActivationFunction af : activationToName.keySet()) {
            nameToActTemp.put(activationToName.get(af), af);
        }
        nameToActivation = Collections.unmodifiableMap(nameToActTemp);

        Map<LossFunction, String> lossToNameTemp = new java.util.LinkedHashMap<>();
        lossToNameTemp.put(new MeanSquaredError(), "MSE");
        lossToName = Collections.unmodifiableMap(lossToNameTemp);

        Map<String, LossFunction> nameToLossTemp = new java.util.LinkedHashMap<>();
        for (LossFunction lf : lossToName.keySet()) {
            nameToLossTemp.put(lossToNameTemp.get(lf), lf);
        }
        nameToLoss = Collections.unmodifiableMap(nameToLossTemp);

        Map<BiasInitializer, String> biasInitializerToNameTemp = new java.util.LinkedHashMap<>();
        biasInitializerToNameTemp.put(new BiasInitializerRandom(), "Random");
        biasInitializerToNameTemp.put(new BiasInitializerNull(), "Null");
        biasInitializerToNameTemp.put(new BiasInitializerVerySmall(), "Very Small");
        biasInitializerToName = Collections.unmodifiableMap(biasInitializerToNameTemp);

        Map<String, BiasInitializer> nameToBiasInitializerTemp = new java.util.LinkedHashMap<>();
        for (BiasInitializer lf : biasInitializerToName.keySet()) {
            nameToBiasInitializerTemp.put(biasInitializerToNameTemp.get(lf), lf);
        }
        nameToBiasInitializer = Collections.unmodifiableMap(nameToBiasInitializerTemp);

        Map<WeightInitializer, String> weightInitializerToNameTemp = new java.util.LinkedHashMap<>();
        weightInitializerToNameTemp.put(new WeightInitializerRandom(), "Random");
        //noinspection SpellCheckingInspection
        weightInitializerToNameTemp.put(new WeightInitializerGlorot(), "Glorot");
        weightInitializerToName = Collections.unmodifiableMap(weightInitializerToNameTemp);

        Map<String, WeightInitializer> nameToWeightInitializerTemp = new java.util.LinkedHashMap<>();
        for (WeightInitializer lf : weightInitializerToName.keySet()) {
            nameToWeightInitializerTemp.put(weightInitializerToNameTemp.get(lf), lf);
        }
        nameToWeightInitializer = Collections.unmodifiableMap(nameToWeightInitializerTemp);
    }

    private static final int[][] wins = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
            {0, 4, 8}, {2, 4, 6}             // diagonals
    };

    private static <T> ENDED_STATUS ended(T[] state, Function<T, ENDED_STATUS> converter) {
        for (int[] win : wins) {
            if (state[win[0]] != null && state[win[0]] != ENDED_STATUS.TIE && state[win[0]] == state[win[1]] && state[win[1]] == state[win[2]]) {
                return converter.apply(state[win[0]]);
            }
        }

        boolean full = true;
        for (T s : state) {
            if (s == null) {
                full = false;
                break;
            }
        }

        if (full)
            return ENDED_STATUS.TIE;

        return null;
    }

    public static ENDED_STATUS globalEnded(PLAYER[][] state) {
        ENDED_STATUS[] localEnds = Arrays.stream(state).map(Utils::localEnded).toArray(ENDED_STATUS[]::new);
        return ended(localEnds, Function.identity());
    }

    public static ENDED_STATUS localEnded(PLAYER[] state) {
        return ended(state, PLAYER_TO_ENDED_STATUS::get);
    }

    public static double calculateAlpha(boolean useDecay, double alpha, int episodes, int ep) {
        if (!useDecay) return alpha;
        return (-1 * Math.pow((1d / episodes * ep), 4) + 1) * alpha;
    }

    public static double calculateEpsilon(boolean useDecay, double epsilon, int ep) {
        if (!useDecay) return epsilon;
        double decayRate = 1d / (1d / 10 * epsilon);
        return epsilon / (1 + decayRate * ep);
    }

    @SuppressWarnings("unused")
    public static String boardToString(GlobalBoard board) {
        return boardToString(board, -1, -1);
    }

    public static String boardToString(GlobalBoard board, int globalCellIndex, int localCellIndex) {
        StringBuilder out = new StringBuilder();
        // Mapping: X -> 'X', O -> 'O', NOT_SET -> '.'
        for (int bigRow = 0; bigRow < 3; bigRow++) {
            for (int innerRow = 0; innerRow < 3; innerRow++) {
                StringBuilder line = new StringBuilder();
                for (int bigCol = 0; bigCol < 3; bigCol++) {
                    int subIndex = bigRow * 3 + bigCol; // 0..8 small boards
                    for (int innerCol = 0; innerCol < 3; innerCol++) {
                        int cellIndex = innerRow * 3 + innerCol; // 0..8 inside small board
                        PLAYER cell = board.getCell(subIndex).getCell(cellIndex);
                        String ch = (cell == PLAYER.X) ? "x"
                                : (cell == PLAYER.O) ? "o" : ".";
                        if (subIndex == globalCellIndex && cellIndex == localCellIndex)
                            ch = (cell == PLAYER.X) ? "✖"
                                    : (cell == PLAYER.O) ? "◯" : ".";
                        line.append(ch);
                        if (innerCol < 2) line.append(' ');
                    }
                    if (bigCol < 2) line.append("  |  ");
                }
                out.append(line).append('\n');
            }
            if (bigRow < 2) {
                out.append("-------+---------+-------").append('\n');
            }
        }
        out.append('\n');
        return out.toString();
    }
}
