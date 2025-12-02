import nn.FFN;
import uttt.GlobalBoard;
import uttt.LocalBoard;

public class UTTT_FFN {
    private final FFN net = new FFN(Main.LAYER_SIZES, Main.HIDDEN_ACTIVATIONS, Main.OUTPUT_ACTIVATION, 100);

    // ---------------------------
    //     CHOOSE ACTION
    // ---------------------------
    public int chooseAction(LocalBoard board, double[] state) {
        // get all available actions for this move
        int[] actions = board.getValidActions();

        // choose random action sometimes
        if (Math.random() < Main.EPSILON)
            return actions[(int) (Math.random() * actions.length)];

        double[] q = net.predictQ(state);

        int bestA = actions[0];
        double bestQ = q[bestA];
        // select action with bestQ but ONLY out of available actions for this move
        if (actions.length > 1)
            for (int i = 1; i < actions.length; i++) {
                int a = actions[i];
                if (q[a] > bestQ) {
                    bestQ = q[a];
                    bestA = a;
                }
            }
        return bestA;
    }

    // ---------------------------
    //     CHOOSE BOARD
    // ---------------------------
    public LocalBoard chooseBoard(GlobalBoard globalBoard, double[] state) {
        LocalBoard[] boards = globalBoard.getRemainingLocalBoards();

        // choose random action sometimes
        if (Math.random() < Main.EPSILON)
            return boards[(int) (Math.random() * boards.length)];

        double[] q = net.predictQ(state);

        LocalBoard bestB = boards[0];
        double bestQ = q[bestB.getIdx()];
        // select action with bestQ but ONLY out of available actions for this move
        if (boards.length > 1)
            for (int i = 1; i < boards.length; i++) {
                LocalBoard a = boards[i];
                if (q[a.getIdx()] > bestQ) {
                    bestQ = q[a.getIdx()];
                    bestB = a;
                }
            }
        return bestB;
    }

    // ---------------------------
    //        NN-UPDATE
    // ---------------------------
    public void train(double[] state, double[] newState, boolean game_over, int action, int[] validActions, int reward) {
        double[] q_s = net.predictQ(state);
        double[] q_sp = net.predictQ(newState);

        double[] target = q_s.clone();

        double targetValue;

        if (game_over) {
            targetValue = reward;
        } else {
            double maxNext = q_sp[validActions[0]];
            if (validActions.length > 1)
                for (int a = 1; a < validActions.length; a++) { //todo: fix that only valid actions are considered (-> what if no valid actions left in local board?!)
                    if (q_sp[validActions[a]] > maxNext)
                        maxNext = q_sp[validActions[a]];
                }
            targetValue = reward + Main.GAMMA * maxNext;
        }


        target[action] = targetValue;

        net.trainFromAction(state, target, Main.ALPHA, Main.LOSS_FUNCTION);
    }
}
