package helper;

import nn.activation.ActivationFunction;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.GlobalBoard;
import uttt.board.Selection;

public class Stats {
    private int o_wins = 0;
    private int x_wins = 0;
    private int ties = 0;

    public void logGameEnd(ENDED_STATUS endedStatus) {
        System.out.println("RESULT: " + endedStatus);
        switch (endedStatus) {
            case O:
                o_wins++;
                break;
            case X:
                x_wins++;
                break;
            default:
                ties++;
        }
    }

    public static void logEpisodeFinished(int ep) {
        System.out.println("Episode finished: " + ep);
    }

    public void logAllEpisodesEnd(ActivationFunction hiddenActivations) {
        // print stats at the end
        System.out.println("\n\n");
        System.out.printf("Stats for HIDDEN_ACTIVATION=%s:\n", hiddenActivations);
        System.out.println("X Wins: " + x_wins);
        System.out.println("O Wins: " + o_wins);
        System.out.println("Ties: " + ties);
    }

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
                        PLAYER cell = board.getCell(new Selection(bigRow, bigCol)).getCell(new Selection(innerRow, innerCol));
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
