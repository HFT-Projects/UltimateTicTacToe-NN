package helper;

import nn.activation.*;
import nn.loss.*;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Utils {
    private Utils() {
    }

    public static final Map<ENDED_STATUS, PLAYER> ENDED_STATUS_TO_PLAYER;
    public static final Map<PLAYER, ENDED_STATUS> PLAYER_TO_ENDED_STATUS;

    public static final Map<ActivationFunction, String> activationToName;
    public static final Map<String, ActivationFunction> nameToActivation;

    public static final Map<LossFunction, String> lossToName;
    public static final Map<String, LossFunction> nameToLoss;

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
}
