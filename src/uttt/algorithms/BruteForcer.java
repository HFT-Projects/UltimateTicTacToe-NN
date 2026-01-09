package uttt.algorithms;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

public abstract class BruteForcer {
    protected int xWins = 0;
    protected int oWins = 0;
    protected int ties = 0;
    protected long exploredStates = 0;
    protected int endStates = 0;

    public int getxWins() {
        return xWins;
    }

    public int getoWins() {
        return oWins;
    }

    public int getTies() {
        return ties;
    }

    public void resetStats() {
        xWins = 0;
        oWins = 0;
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
    protected final boolean earlyReturn;
    protected final int maxDepth;
    protected final int maxStates;
    protected final boolean printProgress;
    /**
     * Number of terminating (end) states found after program terminates
     */
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
                oWins++;
                break;
            case X:
                xWins++;
                break;
            case TIE:
                ties++;
                break;
            default:
                break;
        }
    }

    /**
     * This assumes that the state is NEVER a full board
     */
    @SuppressWarnings("unused")
    public abstract void bruteForce(PLAYER[][] gb, PLAYER player, byte idx);
}
