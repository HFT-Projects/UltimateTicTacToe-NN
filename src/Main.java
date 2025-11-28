import nn.activation.ActivationFunction;
import nn.activation.IdentityFunction;
import nn.activation.SigmoidFunction;
import nn.loss.LossFunction;
import nn.loss.MeanSquaredError;

import uttt.Board.PLAYER;
import uttt.Board.ENDED_STATUS;
import uttt.Board;
import uttt.GlobalBoard;
import uttt.LocalBoard;

class Main {
    static final int ACTIONS = 9;
    static final int EPISODES = 1000;
    static final double GAMMA = 0.9;
    static final double EPSILON = 0.2;

    // repeating board selection inside the input to increase weight of this part
    static final int STATE_BOARD_SELECTION_MULTIPLIER = 3;

    static final int INPUT_SIZE = 18 * 9 + 9 * STATE_BOARD_SELECTION_MULTIPLIER;
    static final int OUTPUT_SIZE = ACTIONS;
    static final int[] LAYER_SIZES = {INPUT_SIZE, 512, OUTPUT_SIZE};
    static final ActivationFunction HIDDEN_ACTIVATIONS = new SigmoidFunction();
    static final ActivationFunction OUTPUT_ACTIVATION = new IdentityFunction(); // SHOULD NOT BE CHANGED
    static final double ALPHA = 0.09;
    static final LossFunction LOSS_FUNCTION = new MeanSquaredError();

    static UTTT_FFN netX = new UTTT_FFN(PLAYER.X);
    static UTTT_FFN netO = new UTTT_FFN(PLAYER.O);

    static int x_wins = 0;
    static int o_wins = 0;
    static int ties = 0;

    static void main() {
        System.out.println("Episode " + 0);

        for (int ep = 1; ep <= EPISODES; ep++) {
            runEpisode(ep);
            System.out.println("Episode finished: " + ep);
            if (Math.random() > 0.5) {
                UTTT_FFN tmp = netX;
                netX = netO;
                netO = tmp;
            }
        }
    }

    private static void runEpisode(int epNum) {
        // play board
        GlobalBoard globalBoard = new GlobalBoard();

        // local (inner) board which is next played by player X
        LocalBoard localBoardX = globalBoard.getCell((int) (Math.random() * 9));
        LocalBoard localBoardO;
        MoveResult resultX;
        MoveResult resultO = null;

        while (true) {
            // let the NN make a move for player X
            resultX = move(localBoardX, globalBoard, netX, PLAYER.X, epNum == EPISODES);

            // exit if game has ended
            if (resultX.endedStatus != null) {
                // pre-pone training if game ended
                netX.train(resultX.oldState, resultX.newState, true, resultX.action, null, resultX.reward);
                if (resultO != null)
                    netO.train(resultO.oldState, resultX.newState, true, resultO.action, null, resultO.reward);

                System.out.println(boardToString(globalBoard));
                System.out.println("WON: " + resultX.endedStatus);
                switch (resultX.endedStatus) {
                    case O:
                        o_wins++;
                        break;
                    case X:
                        x_wins++;
                        break;
                    default:
                        ties++;
                }
                break;
            }

            // select which local board they players have to play next
            // based on the last action of player O if that board is still available
            localBoardO = globalBoard.getRemainingLocalBoard(globalBoard.getCell(resultX.action));
            // train net x now that new state is known
            if (resultO != null)
                netO.train(resultO.oldState, resultX.newState, false, resultO.action, localBoardO.getValidActions(), resultO.reward);


            // let the NN make a move for player O
            resultO = move(localBoardO, globalBoard, netO, PLAYER.O, epNum == EPISODES);

            // exit if game has ended
            if (resultO.endedStatus != null) {
                // pre-pone training if game ended
                netO.train(resultO.oldState, resultO.newState, true, resultO.action, null, resultO.reward);
                netX.train(resultX.oldState, resultO.newState, true, resultX.action, null, resultX.reward);
                System.out.println(boardToString(globalBoard));
                System.out.println("WON: " + resultO.endedStatus);
                switch (resultO.endedStatus) {
                    case O:
                        o_wins++;
                        break;
                    case X:
                        x_wins++;
                        break;
                    default:
                        ties++;
                }
                break;
            }

            // select which local board they players have to play next
            // based on the last action of player O if that board is still available
            localBoardX = globalBoard.getRemainingLocalBoard(globalBoard.getCell(resultO.action));
            // train net x now that new state is known
            netX.train(resultX.oldState, resultO.newState, false, resultX.action, localBoardX.getValidActions(), resultX.reward);
        }

        // print stats at the end
        if (epNum == EPISODES) {
            System.out.printf("Stats for HIDDEN_ACTIVATION=%s:%n", HIDDEN_ACTIVATIONS);
            System.out.println("X Wins: " + x_wins);
            System.out.println("O Wins: " + o_wins);
            System.out.println("Ties: " + ties);
        }
    }

    private record MoveResult(int action, int reward, ENDED_STATUS endedStatus, double[] oldState, double[] newState) {
    }

    private static MoveResult move(LocalBoard localBoard, GlobalBoard globalBoard, UTTT_FFN net, PLAYER player, boolean print) {
        // encode state for NN
        double[] state = getStateWithBoardSelection(globalBoard, localBoard);

        int localBoardIndex = 0;
        for (int i = 0; i < 9; i++) {
            if (globalBoard.getCell(i) == localBoard)
                localBoardIndex = i;
        }

        // let the NN predict the best action
        int action = net.move(localBoard, state);

        if (print)
            System.out.println(boardToString(globalBoard, localBoardIndex, action));

        double[] newState = getStateWithBoardSelection(globalBoard, globalBoard.getCell(action));

        ENDED_STATUS endedStatus = globalBoard.ended();

        int reward = calculateReward(endedStatus, localBoard.ended(), player);

        return new MoveResult(action, reward, endedStatus, state, newState);
    }


    // ---------------------------
    //        HELPER METHODS
    // ---------------------------
    private static double[] getStateWithBoardSelection(GlobalBoard globalBoard, LocalBoard localBoard) {
        double[] state = globalBoard.getStateDouble();
        double[] extendedState = new double[state.length + 9 * STATE_BOARD_SELECTION_MULTIPLIER];

        // copy original state into extendedState at the end
        System.arraycopy(state, 0, extendedState, 9 * STATE_BOARD_SELECTION_MULTIPLIER, state.length);

        double[] boardSelectionState = new double[9];
        for (int i = 0; i < 9; i++) {
            boardSelectionState[i] = (i == localBoard.getIdx()) ? 1.0 : 0.0;
        }
        // repeat board selection STATE_BOARD_SELECTION_MULTIPLIER times at the beginning
        for (int i = 0; i < STATE_BOARD_SELECTION_MULTIPLIER; i++) {
            System.arraycopy(boardSelectionState, 0, extendedState, i * 9, 9);
        }

        return extendedState;
    }

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
        return boardToString(board, -1, -1);
    }

    private static String boardToString(GlobalBoard board, int globalCellIndex, int localCellIndex) {
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
