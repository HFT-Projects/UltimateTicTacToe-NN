package uttt.algorithms;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

public abstract class BruteForcer {
    protected int x_wins = 0;
    protected int o_wins = 0;
    protected int ties = 0;
    protected long exploredStates = 0;
    protected int endStates = 0;

    public int getX_wins() {
        return x_wins;
    }

    public int getO_wins() {
        return o_wins;
    }

    public int getTies() {
        return ties;
    }

    public void resetStats() {
        x_wins = 0;
        o_wins = 0;
        ties = 0;
        exploredStates = 0;
        endStates = 0;
    }

    // ---------------------------
    //     CONFIGURABLE OPTIONS
    // ---------------------------
    /**
     * Only make sense when using breadth search
     */
    public boolean earlyReturn;
    public int maxDepth;
    public int maxStates;
    public boolean printProgress;
    /** Number of terminating (end) states found after program terminates */
    public int maxEndStates;

    public BruteForcer() {
        earlyReturn = false;
        maxDepth = Integer.MAX_VALUE;
        maxStates = Integer.MAX_VALUE;
        printProgress = false;
        maxEndStates = Integer.MAX_VALUE;
    }

    protected void incrementStats(ENDED_STATUS end) {
        switch (end) {
            case O:
                o_wins++;
                break;
            case X:
                x_wins++;
                break;
            case TIE:
                ties++;
                break;
            default:
                break;
        }
    }

    /** This assumes that the state is NEVER a full board */
    public abstract void bruteForce(PLAYER[][] gb, PLAYER player, byte idx);
}
