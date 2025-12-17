package gui.game;

import javafx.scene.layout.GridPane;
import uttt.actor.PLAYER;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class GlobalBoardGUI {
    private final GridPane pane;
    private final LocalBoardGUI[] localBoards = new LocalBoardGUI[9];

    public GlobalBoardGUI() {
        pane = new GridPane();
        pane.setHgap(8);
        pane.setVgap(8);

        for (int bigRow = 0; bigRow < 3; bigRow++) {
            for (int bigCol = 0; bigCol < 3; bigCol++) {
                int global = bigRow * 3 + bigCol;
                localBoards[global] = new LocalBoardGUI();
                pane.add(localBoards[global].getPane(), bigCol, bigRow);
            }
        }
    }

    public GridPane getPane() {
        return pane;
    }

    public void displayState(PLAYER[][] state, Integer lastMoveBoard, Integer lastMoveAction) {
        if (lastMoveBoard == null ^ lastMoveAction == null)
            throw new IllegalArgumentException("Both lastMoveBoard and lastMoveAction should be null or non-null");

        for (int global = 0; global < 9; global++) {
            localBoards[global].displayState(state[global], Objects.equals(global, lastMoveBoard) ? lastMoveAction : null);
        }
    }

    public int selectMove(PLAYER player, int localBoardSel, int[] playableSelections, AtomicReference<Boolean> exit) {
        LocalBoardGUI board = localBoards[localBoardSel];
        return board.selectCell(player, playableSelections, exit);
    }

    public int chooseBoard(PLAYER player, int[] playableBoards, AtomicReference<Boolean> exit) {
        AtomicReference<Integer> selBoard = new AtomicReference<>();
        for (int sel : playableBoards) {
            LocalBoardGUI board = localBoards[sel];
            board.startChooseBoard(player, () -> selBoard.set(sel));
        }

        while (selBoard.get() == null) {
            if (exit.get())
                throw new UncheckedInterruptedException();
            try {
                //noinspection BusyWait
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        for (int sel : playableBoards) {
            LocalBoardGUI board = localBoards[sel];
            board.endChooseBoard();
        }

        return selBoard.get();
    }
}
