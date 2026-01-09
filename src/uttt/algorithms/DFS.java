package uttt.algorithms;

import helper.Utils;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

import java.util.Random;

/**
 * Default behavior is searching path randomly from the "top" until it ends
 */
public class DFS extends BruteForcer {
    private int deepestDepth;
    private final Random r = new Random();

    /**
     * if set to false, make sure to set maxEndStates to Integer.MAX_VALUE otherwise the algorithm will end too early
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean randomTraversal = true;

    public DFS() {
        maxEndStates = 1000;
    }

    @Override
    public void bruteForce(PLAYER[][] gb, PLAYER player, byte idx) {
        if (!randomTraversal)
            depthTraversal(gb, player, idx, 0);
        else {
            while (endStates < maxEndStates) {
                endStates++;
                randomDepthTraversal(gb, player, idx, 0);
                if (printProgress && endStates % 1000 == 0)
                    System.out.println("Searched " + endStates);
            }
        }

        if (printProgress) {
            System.out.printf("Done with depth traversal: searched %d states, %d ended in a TIE, %d in wins for x, %d in wins for o%n", exploredStates, ties, xWins, oWins);
            System.out.printf("Stats: X wins to %.2f%% - O wins to %.2f%% - Ties to %.2f%%",
                    ((float) xWins / endStates) * 100,
                    ((float) oWins / endStates) * 100,
                    ((float) ties / endStates) * 100);
        }
    }

    /**
     * recursive traversal
     */
    private void depthTraversal(PLAYER[][] baseState, PLAYER startPlayer, int localBoardIdx, int currentDepth) {
        if (exploredStates >= maxStates || currentDepth > maxDepth || endStates >= maxEndStates) {
            return;
        }
        exploredStates++;
        if (printProgress && deepestDepth < currentDepth) {
            deepestDepth = currentDepth;
            // System.out.printf("Reached new deepest depth: %d%n", deepestDepth);

        }

        ENDED_STATUS end = Utils.globalEnded(baseState);
        if (end != null) {
            endStates++;
            incrementStats(end);
            return;
        }

        PLAYER[] l = baseState[localBoardIdx];
        if (Utils.localEnded(l) != null) {
            for (int i = 0; i < 9; i++) {
                l = baseState[i];
                if (Utils.localEnded(l) != null)
                    continue;

                traverseAllPaths(l, startPlayer, baseState, currentDepth);
            }
            return;
        }

        traverseAllPaths(l, startPlayer, baseState, currentDepth);
    }

    private void traverseAllPaths(PLAYER[] l, PLAYER currentPlayer, PLAYER[][] baseState, int currentDepth) {
        for (int j = 0; j < 9; j++) {
            if (l[j] != null)
                continue;

            l[j] = currentPlayer;
            PLAYER newPLayer = currentPlayer == PLAYER.O ? PLAYER.X : PLAYER.O;
            depthTraversal(baseState, newPLayer, j, currentDepth + 1);
            // undo the changes made
            l[j] = null;
        }
    }

    /**
     * travels randomly down one path in the tree
     */
    private void randomDepthTraversal(PLAYER[][] baseState, PLAYER startPlayer, int localBoardIdx, int currentDepth) {
        if (exploredStates >= maxStates || currentDepth > maxDepth) {
            return;
        }
        exploredStates++;
        if (printProgress && deepestDepth < currentDepth) {
            deepestDepth = currentDepth;
            System.out.printf("Reached new deepest depth: %d%n", deepestDepth);
        }

        ENDED_STATUS end = Utils.globalEnded(baseState);
        if (end != null) {
            incrementStats(end);
            return;
        }

        PLAYER[] l = baseState[localBoardIdx];
        if (Utils.localEnded(l) != null) {
            // break after one not ended board is found and one path is followed
            for (; ; ) {
                int i = r.nextInt(9);
                l = baseState[i];
                if (Utils.localEnded(l) != null)
                    continue;

                traverseRandomPath(l, startPlayer, baseState, currentDepth);
                return;
            }
        }

        traverseRandomPath(l, startPlayer, baseState, currentDepth);
    }

    private void traverseRandomPath(PLAYER[] l, PLAYER currentPlayer, PLAYER[][] baseState, int currentDepth) {
        int idx = r.nextInt(9);
        while (l[idx] != null) {
            idx = r.nextInt(9);
        }
        l[idx] = currentPlayer;
        PLAYER newPlayer = currentPlayer == PLAYER.O ? PLAYER.X : PLAYER.O;
        randomDepthTraversal(baseState, newPlayer, idx, currentDepth + 1);
        // undo the changes made
        l[idx] = null;
    }
}
