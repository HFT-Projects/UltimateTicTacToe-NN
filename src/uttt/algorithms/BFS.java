package uttt.algorithms;

import helper.Utils;
import org.jspecify.annotations.Nullable;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

import java.util.*;

@SuppressWarnings("unused")
public class BFS extends BruteForcer {
    private record Node(List<int[]> actions) {
    }

    private final Queue<Node> toExplore = new ArrayDeque<>();

    /**
     * Only gets set when earlyReturn = true
     */
    private Node fastestWayToWin;

    public BFS() {
        super();
        fastestWayToWin = null;
    }

    @Override
    public void bruteForce(PLAYER[][] gb, PLAYER player, byte idx) {
        breadthTraversal(null, gb, player, idx);

        while (toExplore.peek() != null && toExplore.peek().actions.size() <= maxDepth) {
            breadthTraversal(toExplore.poll(), gb, player, idx);
            exploredStates++;
            if (exploredStates % 1000000 == 0 && printProgress) {
                System.out.println("Done 1m items. Progress: " + exploredStates);
            }

            if (exploredStates >= maxStates || endStates >= maxEndStates) {
                break;
            }

            if (fastestWayToWin != null) {
                System.out.println("Fastest way to win: ");
                fastestWayToWin.actions.forEach(a -> System.out.print(Arrays.toString(a)));
                System.out.println();
                break;
            }
        }

        System.out.printf("Done with breadth traversal: searched %d states, %d ended in a TIE, %d in wins for x, %d in wins for o", exploredStates, ties, xWins, oWins);
    }

    private void breadthTraversal(@Nullable Node n, PLAYER[][] baseState, PLAYER startPlayer, int localBoardIdx) {
        PLAYER currentPlayer = startPlayer;
        int currentIndex = localBoardIdx;
        if (n != null) {
            for (byte i = 0; i < n.actions.size(); i++) {
                int[] idx = n.actions.get(i);
                PLAYER[] l = baseState[idx[0]];
                l[idx[1]] = currentPlayer;
                currentPlayer = currentPlayer == PLAYER.X ? PLAYER.O : PLAYER.X;

                // if last move get new localBoardIndex
                if (i == n.actions.size() - 1) {
                    currentIndex = n.actions.get(i)[1];
                }
            }
        }

        // break condition
        ENDED_STATUS globalStatus;
        if (n != null && (globalStatus = Utils.globalEnded(baseState)) != null) {
            if (earlyReturn && globalStatus == Utils.PLAYER_TO_ENDED_STATUS.get(startPlayer)) {
                fastestWayToWin = n;
            }
            endStates++;
            incrementStats(globalStatus);
            // undo all results
            for (int[] action : n.actions) {
                baseState[action[0]][action[1]] = null;
            }
            return;
        }

        // l equals the next board that should be played
        PLAYER[] l = baseState[currentIndex];
        if (Utils.localEnded(l) != null) {
            // player can choose any board
            for (byte j = 0; j < 9; j++) {
                if (Utils.localEnded(baseState[j]) != null)
                    continue;

                for (byte i = 0; i < 9; i++) {
                    if (baseState[j][i] == null) {
                        ArrayList<int[]> actions = new ArrayList<>();
                        if (n != null)
                            actions.addAll(n.actions);

                        actions.add(new int[]{j, i});
                        toExplore.add(new Node(actions));
                    }
                }
            }
        } else {
            for (byte i = 0; i < 9; i++) {
                if (l[i] == null) {
                    ArrayList<int[]> actions = new ArrayList<>();
                    if (n != null)
                        actions.addAll(n.actions);

                    actions.add(new int[]{currentIndex, i});
                    toExplore.add(new Node(actions));
                }
            }
        }

        // undo all changes to the state
        if (n != null) {
            for (int[] action : n.actions) {
                baseState[action[0]][action[1]] = null;
            }
        }
    }
}
