package helper;

import nn.activation.*;
import nn.loss.*;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.Selection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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


    public static int selectionToInt(Selection sel) {
        return sel.idxRow() * 3 + sel.idxColumn();
    }

    public static Selection intToSelection(int index) {
        return new Selection(index / 3, index % 3);
    }
}
