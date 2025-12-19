package uttt.actor;

import uttt.algorithms.DFS;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DFSActor extends Actor {
    private final DFS algo;
    private final int strength;

    public DFSActor(PLAYER player, int strength) {
        this.strength = strength;
        super(player);
        algo = new DFS();
        algo.maxEndStates = 100 * strength;
    }

    @Override
    public int move(PLAYER[][] state, int localBoardSel, int[] playableActions) {
        algo.maxEndStates = 100 * strength;

        Map<Integer, Double> scorePerSelection = new HashMap<>();
        PLAYER[] l = state[localBoardSel];
        for (int action : playableActions) {
            // simulate move
            l[action] = this.getPlayer();
            // calculate win chance
            algo.bruteForce(state, this.getPlayer() == PLAYER.X ? PLAYER.O : PLAYER.X, (byte)action);
            // check if win chance is highest for this selection -> set if yes
            scorePerSelection.put(action, getScore());
            // undo move
            l[action] = null;
            algo.resetStats();
        }

        Optional<Map.Entry<Integer, Double>> chosenSelection = scorePerSelection.entrySet().stream().max(Map.Entry.comparingByValue());
        if (chosenSelection.isEmpty())
            throw new RuntimeException("Could not calculate win chance!");

        return chosenSelection.get().getKey();
    }

    @Override
    public int chooseBoard(PLAYER[][] state, int[] playableBoards) {
        algo.maxEndStates = 10 * strength;

        Map<Integer, Double> scorePerBoard = new HashMap<>();
        for (int playableBoard : playableBoards) {
            PLAYER[] l = state[playableBoard];
            for (int j = 0; j < 9; j++) {
                if (l[j] != null)
                    continue;

                // simulate move
                l[j] = this.getPlayer();
                // calculate win chance
                algo.bruteForce(state, this.getPlayer() == PLAYER.X ? PLAYER.O : PLAYER.X, (byte)j);
                // check if win chance is highest for this selection -> set if yes
                double score = getScore();
                if (!scorePerBoard.containsKey(playableBoard) || scorePerBoard.get(playableBoard) < score) {
                    scorePerBoard.put(playableBoard, score);
                }
                // undo move
                l[j] = null;
                algo.resetStats();
            }
        }
        Optional<Map.Entry<Integer, Double>> chosenBoard = scorePerBoard.entrySet().stream().max(Map.Entry.comparingByValue());
        if (chosenBoard.isEmpty())
            throw new RuntimeException("Could not calculate win chance!");

        return chosenBoard.get().getKey();
    }

    private double getScore() {
        return getWins() + (1f/3 * (double)algo.getTies());
    }

    private int getWins() {
        return switch (this.getPlayer()) {
            case X -> algo.getX_wins();
            case O -> algo.getO_wins();
        };
    }
}
