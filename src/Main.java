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
    static final String HIDDEN_ACTIVATIONS = "sigm";
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
            if (Math.random() > 0.5) {
                UTTT_FFN tmp = netX;
                netX = netO;
                netO = tmp;
            }
        }
    }

    private static void runEpisode() {
        // play board
        GlobalBoard globalBoard = new GlobalBoard();

        // local (inner) board which is next played by player X
        LocalBoard localBoardX = globalBoard.getCell((int) (Math.random() * 9));
        LocalBoard localBoardO;
        MoveResult resultX;
        MoveResult resultO = null;

        while (true) {
            // let the NN make a move for player X
            resultX = move(localBoardX, globalBoard, netX, PLAYER.X);

            // exit if game has ended
            if (resultX.endedStatus != null) {
                System.out.println(boardToString(globalBoard));
                System.out.println("WON: " + resultX.endedStatus);
                break;
            }

            // select which local board they players have to play next
            // based on the last action of player O if that board is still available
            localBoardO = globalBoard.getRemainingLocalBoard(globalBoard.getCell(resultX.action));
            // train net x now that new state is known
            if (resultO != null)
                netO.train(resultO.oldState, resultX.newState, resultO.endedStatus != null, resultO.action, localBoardO.getValidActions(), resultO.reward);


            // let the NN make a move for player O
            resultO = move(localBoardO, globalBoard, netO, PLAYER.O);

            // exit if game has ended
            if (resultO.endedStatus != null) {
                System.out.println(boardToString(globalBoard));
                System.out.println("WON: " + resultO.endedStatus);
                break;
            }

            // select which local board they players have to play next
            // based on the last action of player O if that board is still available
            localBoardX = globalBoard.getRemainingLocalBoard(globalBoard.getCell(resultO.action));
            // train net x now that new state is known
            netX.train(resultX.oldState, resultO.newState, resultX.endedStatus != null, resultX.action, localBoardX.getValidActions(), resultX.reward);
        }
    }

    private record MoveResult(int action, int reward, ENDED_STATUS endedStatus, double[] oldState, double[] newState) {}

    private static MoveResult move(LocalBoard localBoard, GlobalBoard globalBoard, UTTT_FFN net, PLAYER player) {
        // encode state for NN
        double[] state = localBoard.getStateDouble();

        // let the NN predict the best action
        int action = net.move(localBoard);

        // System.out.println(boardToString(globalBoard));

        double[] newState = localBoard.getStateDouble();

        ENDED_STATUS endedStatus = globalBoard.ended();

        int reward = calculateReward(endedStatus, localBoard.ended(), player);

        return new MoveResult(action, reward, endedStatus, state, newState);
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