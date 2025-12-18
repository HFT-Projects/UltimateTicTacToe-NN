package uttt.actor;

import nn.FFN;

import nn.trainer.FFNTrainer;
import uttt.board.ENDED_STATUS;
import uttt.observer.Event;
import helper.Utils;

public class NNActor extends Actor {
    private enum CELL_STATE {
        YOU,
        ENEMY,
        NOT_SET
    }

    private final FFN net;
    private final FFNTrainer trainer;
    // repeating board selection inside the input to increase weight of this part
    private final int stateBoardSelectionMultiplier;

    private final double alpha; // learning rate
    private final double gamma; // discount factor
    private final double epsilon; // exploration rate

    private Integer lastAction = null;
    private double[] oldState = null;
    private ENDED_STATUS oldLocalEndedStatus = null;
    private boolean eventHandlerRegistered = false;

    public NNActor(PLAYER player, FFN net, FFNTrainer trainer, double alpha, double gamma, double epsilon) {
        super(player);
        this.net = net;
        this.trainer = trainer;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.stateBoardSelectionMultiplier = (net.layerSizes[0] - 18 * 9) / 9;

        // assertions
        if (net.layerSizes[0] != 18 * 9 + 9 * stateBoardSelectionMultiplier)
            throw new IllegalArgumentException("Input layer size of the neural network does not match the expected size based on the state representation.");
        if (net.layerSizes[net.layerSizes.length - 1] != 9)
            throw new IllegalArgumentException("Output layer size of the neural network must be 9 (one for each possible action).");
    }

    // ---------------------------
    //     CHOOSE ACTION
    // ---------------------------
    @Override
    public int move(PLAYER[][] state, int localBoardSel, int[] playableActions) {
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
    public int chooseBoard(PLAYER[][] state, int[] playableBoards) {
        return predict(getStateWithBoardSelection(convertPlayerStateToCellState(state), null), playableBoards);
    }

    // ---------------------------
    //     NN PREDICT ACTION
    // ---------------------------
    private int predict(double[] state, int[] objects) {
        if (objects.length < 1)
            throw new IllegalArgumentException("No playable actions available!");

        // choose random action sometimes
        if (Math.random() < epsilon)
            return objects[(int) (Math.random() * objects.length)];

        double[] q = net.predictQ(state);

        int bestObj = objects[0];
        double bestQ = q[bestObj];
        // select action with bestQ but ONLY out of available actions for this move
        for (int i = 1; i < objects.length; i++) {
            int action = objects[i];
            if (q[action] > bestQ) {
                bestQ = q[action];
                bestObj = action;
            }
        }

        return bestObj;
    }

    // ---------------------------
    //        NN-UPDATE
    // ---------------------------
    private void train(double[] state, double[] newState, ENDED_STATUS globalEndedStatus, int action, int[] playableActions) {
        if (!eventHandlerRegistered)
            //noinspection SpellCheckingInspection
            throw new IllegalStateException("Event handler not registered! Make sure to register the event handler of the NNActor in the Game before starting the game.");

        int reward = calculateReward(globalEndedStatus, oldLocalEndedStatus);

        double[] q_s = net.predictQ(state);
        double[] q_sp = net.predictQ(newState);

        double[] target = q_s.clone();

        double targetValue;
        if (globalEndedStatus != null) {
            targetValue = reward;
        } else {
            double maxNext = q_sp[predict(newState, playableActions)];
            targetValue = reward + gamma * maxNext;
        }


        target[action] = targetValue;

        net.train(trainer, state, target, alpha);
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
                int idx1 = i * 9 * 2 + 2 * k;
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

    private double[] getStateWithBoardSelection(CELL_STATE[][] state, Integer localBoardSel) {
        double[] doubleState = convertCellStateToDoubleState(state);
        double[] extendedDoubleState = new double[doubleState.length + 9 * stateBoardSelectionMultiplier];

        // copy original doubleState into extendedDoubleState at the end
        System.arraycopy(doubleState, 0, extendedDoubleState, 9 * stateBoardSelectionMultiplier, doubleState.length);

        double[] boardSelectionState = new double[9];
        int idx = (localBoardSel == null) ? -1 : localBoardSel;
        for (int i = 0; i < 9; i++) {
            boardSelectionState[i] = (i == idx) ? 1.0 : 0.0;
        }
        // repeat board selection STATE_BOARD_SELECTION_MULTIPLIER times at the beginning
        for (int i = 0; i < stateBoardSelectionMultiplier; i++) {
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
