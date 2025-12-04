package uttt.actor;

import nn.FFN;
import nn.activation.ActivationFunction;
import nn.activation.IdentityFunction;
import nn.activation.SigmoidFunction;
import nn.loss.LossFunction;
import nn.loss.MeanSquaredError;

import uttt.board.ENDED_STATUS;
import uttt.board.Selection;
import uttt.observer.Event;
import helper.Utils;

public class NNActor extends Actor {
    private enum CELL_STATE {
        YOU,
        ENEMY,
        NOT_SET
    }

    private static final int ACTIONS = 9;
    private static final double GAMMA = 0.9;
    private static final double EPSILON = 0.2;

    // repeating board selection inside the input to increase weight of this part
    private static final int STATE_BOARD_SELECTION_MULTIPLIER = 3;

    private static final int INPUT_SIZE = 18 * 9 + 9 * STATE_BOARD_SELECTION_MULTIPLIER;
    private static final int OUTPUT_SIZE = ACTIONS;
    private static final int[] LAYER_SIZES = {INPUT_SIZE, 512, OUTPUT_SIZE};
    public static final ActivationFunction HIDDEN_ACTIVATIONS = new SigmoidFunction();
    private static final ActivationFunction OUTPUT_ACTIVATION = new IdentityFunction(); // SHOULD NOT BE CHANGED
    private static final double ALPHA = 0.09;
    private static final LossFunction LOSS_FUNCTION = new MeanSquaredError();

    private final FFN net = new FFN(LAYER_SIZES, HIDDEN_ACTIVATIONS, OUTPUT_ACTIVATION, 100);
    private Selection lastAction = null;
    private double[] oldState = null;
    private ENDED_STATUS oldLocalEndedStatus = null;
    private boolean eventHandlerRegistered = false;

    public NNActor(PLAYER player) {
        super(player);
    }

    // ---------------------------
    //     CHOOSE ACTION
    // ---------------------------
    @Override
    public Selection move(PLAYER[][] state, Selection localBoardSel, Selection[] playableActions) {
        double[] stateDouble = getStateWithBoardSelection(convertPlayerStateToCellState(state), localBoardSel);
        if (lastAction == null ^ oldState == null)
            throw new IllegalStateException("Inconsistent internal state: lastAction and oldState should both be null or both be non-null.");
        if (lastAction != null)
            train(oldState, getStateWithBoardSelection(convertPlayerStateToCellState(state), localBoardSel), null, lastAction, playableActions);
        oldState = stateDouble;
        return lastAction = predict(stateDouble, playableActions);
    }

    // ---------------------------
    //     CHOOSE BOARD
    // ---------------------------
    @Override
    public Selection chooseBoard(PLAYER[][] state, Selection[] playableBoards) {
        return predict(getStateWithBoardSelection(convertPlayerStateToCellState(state), null), playableBoards);
    }

    // ---------------------------
    //     NN PREDICT ACTION
    // ---------------------------
    private Selection predict(double[] state, Selection[] objects) {
        if (objects.length < 1)
            throw new IllegalArgumentException("No playable actions available!");

        // choose random action sometimes
        if (Math.random() < EPSILON)
            return objects[(int) (Math.random() * objects.length)];

        double[] q = net.predictQ(state);

        Selection bestObj = objects[0];
        double bestQ = q[Utils.selectionToInt(bestObj)];
        // select action with bestQ but ONLY out of available actions for this move
        for (int i = 1; i < objects.length; i++) {
            Selection action = objects[i];
            if (q[Utils.selectionToInt(action)] > bestQ) {
                bestQ = q[Utils.selectionToInt(action)];
                bestObj = action;
            }
        }

        return bestObj;
    }

    // ---------------------------
    //        NN-UPDATE
    // ---------------------------
    private void train(double[] state, double[] newState, ENDED_STATUS globalEndedStatus, Selection action, Selection[] playableActions) {
        if (!eventHandlerRegistered)
            throw new IllegalStateException("Event handler not registered! Make sure to register the event handler of the NNActor in the Game before starting the game.");

        int reward = calculateReward(globalEndedStatus, oldLocalEndedStatus);

        double[] q_s = net.predictQ(state);
        double[] q_sp = net.predictQ(newState);

        double[] target = q_s.clone();

        double targetValue;
        if (globalEndedStatus != null) {
            targetValue = reward;
        } else {
            double maxNext = q_sp[Utils.selectionToInt(predict(newState, playableActions))];
            targetValue = reward + GAMMA * maxNext;
        }


        target[Utils.selectionToInt(action)] = targetValue;

        net.trainFromAction(state, target, ALPHA, LOSS_FUNCTION);
    }

    // ---------------------------
    //        EVENT HANDLER
    // ---------------------------
    // handles training when game ends & updates oldLocalEndedStatus after each of our moves
    public void eventHandler(Event event) {
        eventHandlerRegistered = true;
        if (event.globalEndedStatus() != null) {
            PLAYER[][] newState = event.newState();
            train(oldState, getStateWithBoardSelection(convertPlayerStateToCellState(newState), null), event.globalEndedStatus(), lastAction, null);
        } else if (event.player() == getPlayer()) {
            oldLocalEndedStatus = event.localEndedStatus();
        }
    }

    // ---------------------------
    //        HELPER METHODS
    // ---------------------------

    // converts PLAYER[][] state to CELL_STATE[][] state to make it easier to work with for the NN
    // because we only care about "YOU", "ENEMY" and "NOT_SET" and not the actual PLAYER values
    private CELL_STATE[][] convertPlayerStateToCellState(PLAYER[][] state) {
        CELL_STATE[][] cellState = new CELL_STATE[9][9];
        for (int i = 0; i < 9; i++) {
            for (int k = 0; k < 9; k++) {
                if (state[i][k] == getPlayer()) {
                    cellState[i][k] = CELL_STATE.YOU;
                } else if (state[i][k] != null) {
                    cellState[i][k] = CELL_STATE.ENEMY;
                } else {
                    cellState[i][k] = CELL_STATE.NOT_SET;
                }
            }
        }
        return cellState;
    }

    private double[] convertCellStateToDoubleState(CELL_STATE[][] state) {
        double[] stateDouble = new double[9 * 9 * 2];
        for (int i = 0; i < 9; i++) {
            for (int k = 0; k < 9; k++) {
                int idx1 = i * 9 + 2 * k;
                int idx2 = idx1 + 1;
                if (state[i][k] == CELL_STATE.YOU) {
                    stateDouble[idx1] = 1;
                    stateDouble[idx2] = 0;
                } else if (state[i][k] == CELL_STATE.ENEMY) {
                    stateDouble[idx1] = 0;
                    stateDouble[idx2] = 1;
                } else {
                    stateDouble[idx1] = 0;
                    stateDouble[idx2] = 0;
                }
            }
        }
        return stateDouble;
    }

    private double[] getStateWithBoardSelection(CELL_STATE[][] state, Selection localBoardSel) {
        double[] doubleState = convertCellStateToDoubleState(state);
        double[] extendedDoubleState = new double[doubleState.length + 9 * STATE_BOARD_SELECTION_MULTIPLIER];

        // copy original doubleState into extendedDoubleState at the end
        System.arraycopy(doubleState, 0, extendedDoubleState, 9 * STATE_BOARD_SELECTION_MULTIPLIER, doubleState.length);

        double[] boardSelectionState = new double[9];
        int idx = (localBoardSel == null) ? -1 : Utils.selectionToInt(localBoardSel);
        for (int i = 0; i < 9; i++) {
            boardSelectionState[i] = (i == idx) ? 1.0 : 0.0;
        }
        // repeat board selection STATE_BOARD_SELECTION_MULTIPLIER times at the beginning
        for (int i = 0; i < STATE_BOARD_SELECTION_MULTIPLIER; i++) {
            System.arraycopy(boardSelectionState, 0, extendedDoubleState, i * 9, 9);
        }

        return extendedDoubleState;
    }

    private int calculateReward(ENDED_STATUS globalEnded, ENDED_STATUS localEnded) {
        int reward = -1;
        if (Utils.ENDED_STATUS_TO_PLAYER.get(globalEnded) == getPlayer())
            reward = 100;
        else if (Utils.ENDED_STATUS_TO_PLAYER.get(localEnded) == getPlayer()) {
            reward = 5;
        }
        return reward;
    }
}
