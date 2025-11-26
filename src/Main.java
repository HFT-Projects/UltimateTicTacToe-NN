import nn.LossFunction;
import nn.MeanSquaredError;

import uttt.Board.PLAYER;
import uttt.Board.ENDED_STATUS;
import uttt.Board;
import uttt.GlobalBoard;
import uttt.LocalBoard;

class Main {
    static final int ACTIONS = 9;
    static final int EPISODES = 10000;
    static final double GAMMA = 0.9;
    static final double EPSILON = 0.2;

    static final int INPUT_SIZE = 18;
    static final int OUTPUT_SIZE = ACTIONS;
    static final int[] LAYER_SIZES = {INPUT_SIZE, 100, OUTPUT_SIZE};
    static final String HIDDEN_ACTIVATIONS = "relu";
    static final String OUTPUT_ACTIVATION = "none";
    static final double ALPHA = 0.09;
    static final LossFunction LOSS_FUNCTION = new MeanSquaredError();

    static UTTT_FFN netX = new UTTT_FFN(PLAYER.X);
    static UTTT_FFN netO = new UTTT_FFN(PLAYER.O);
    static int episode = 1;


    void main() {
        System.out.println("Episode " + 0);

        for (int ep = 1; ep <= EPISODES; ep++) {
            runEpisode();
            System.out.println("Episode finished: " + ep);
            episode++;
        }
    }

    private static void runEpisode() {
        // play board
        GlobalBoard globalBoard = new GlobalBoard();

        // local (inner) board which is next played by player X
        LocalBoard localBoardX = globalBoard.getCell((int) (Math.random() * 9));
        // local (inner) board which is next played by player O
        LocalBoard localBoardO = globalBoard.getCell((int) (Math.random() * 9));

        while (true) {
            ENDED_STATUS endedStatus;

            // let the NN make a move for player X
            int actionX = move(localBoardX, globalBoard, netX, PLAYER.X);

            // exit if game has ended
            endedStatus = globalBoard.ended();
            if (endedStatus != null) {
                System.out.println(boardToString(globalBoard));
                System.out.println("WON: " + endedStatus);
                break;
            }


            // let the NN make a move for player O
            int actionO = move(localBoardO, globalBoard, netO, PLAYER.O);

            // exit if game has ended
            endedStatus = globalBoard.ended();
            if (endedStatus != null) {
                System.out.println(boardToString(globalBoard));
                System.out.println("WON: " + endedStatus);
                break;
            }

            // select which local board they players have to play next
            localBoardX = globalBoard.getCell(actionX);
            localBoardO = globalBoard.getCell(actionO);
        }
    }

    public static int move(LocalBoard localBoard, GlobalBoard globalBoard, UTTT_FFN net, PLAYER player) {
        // check if selected local board is playable. If not -> select by random
        localBoard = globalBoard.getRemainingLocalBoard(localBoard);

        // encode state for NN
        double[] state = localBoard.getStateDouble();

        // let the NN predict the best action
        int action = net.move(localBoard);

        // System.out.println(boardToString(globalBoard));

        double[] newState = localBoard.getStateDouble();

        ENDED_STATUS endedStatus = globalBoard.ended();

        int reward = calculateReward(endedStatus, localBoard.ended(), player);

        net.train(state, newState, endedStatus != null, action, reward);

        return action;
    }


    // ---------------------------
    //        HELPER METHODS
    // ---------------------------
    private static int calculateReward(ENDED_STATUS globalEnded, ENDED_STATUS localEnded, PLAYER player) {
        int reward = -1;
        if (Board.ENDED_STATUS_TO_PLAYER.get(globalEnded) == player)
            reward = 100;
        else if (Board.ENDED_STATUS_TO_PLAYER.get(localEnded) == player) {
            reward = 5;
        }
        return reward;
    }

    private static String boardToString(GlobalBoard board) {
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
                        char ch = (cell == PLAYER.X) ? 'X'
                                : (cell == PLAYER.O) ? 'O' : '.';
                        line.append(ch);
                        if (innerCol < 2) line.append(' ');
                    }
                    if (bigCol < 2) line.append("  |  ");
                }
                out.append(line).append('\n');
            }
            if (bigRow < 2) {
                out.append("---------+-----------+---------").append('\n');
            }
        }
        out.append('\n');
        return out.toString();
    }
}