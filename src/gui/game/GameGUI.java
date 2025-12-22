package gui.game;

import gui.utils.GUIUtils;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uttt.actor.*;
import uttt.board.ENDED_STATUS;
import uttt.storage.Move;
import uttt.storage.MoveHistoryGenerator;
import uttt.storage.StorageManager;

import java.util.concurrent.atomic.AtomicReference;

public class GameGUI {
    public enum GAME_MODE {NN_VS_NN, HUMAN_VS_NN, NN_VS_ALGORITHM, HUMAN_VS_ALGORITHM}

    public enum HUMAN_PLAYER_SYMBOL {X, O, RANDOM}

    private final FlowPane pane;
    private final GlobalBoardGUI globalBoard = new GlobalBoardGUI();
    private final Label statusLabel;
    private final Label stepLabel;

    private boolean ran = false;
    GUIActor guiActor = null;
    private final MoveHistoryGenerator moveHistoryGenerator = new MoveHistoryGenerator();
    private Move[] moveHistory = null;
    private PLAYER[][] state = new PLAYER[9][9];
    private int currentStep = 0;
    private final AtomicReference<Boolean> exit = new AtomicReference<>(false);

    public GameGUI() {

        pane = new FlowPane();
        pane.setOrientation(Orientation.VERTICAL);
        pane.setAlignment(Pos.TOP_CENTER);

        statusLabel = new Label("Click 'Start Game' to simulate a game");
        statusLabel.setFont(Font.font("Arial", 16));
        statusLabel.setTextFill(Color.WHITE);

        stepLabel = new Label("Move: - / -");
        stepLabel.setFont(Font.font("Arial", 14));
        stepLabel.setTextFill(Color.LIGHTGRAY);

        pane.getChildren().setAll(globalBoard.getPane(), statusLabel, stepLabel);
    }

    public void run(@NonNull Actor actorX, @NonNull Actor actorO, @Nullable GUIActor guiActor, Runnable onComplete) {
        if (ran)
            throw new RuntimeException("GameGUI can only run / load once per instance.");
        ran = true;

        this.guiActor = guiActor;

        uttt.Game game = new uttt.Game(actorX, actorO);
        if (actorX instanceof NNActor)
            game.addObserver(((NNActor) actorX)::eventHandler);
        if (actorO instanceof NNActor)
            game.addObserver(((NNActor) actorO)::eventHandler);

        game.addObserver(e -> {
            moveHistoryGenerator.handleEvent(e);
            if (guiActor != null)
                Platform.runLater(this::goToEnd);
        });

        if (guiActor != null)
            GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Game running..."));
        else
            GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("Game running..."));

        try {
            // run the game loop
            ENDED_STATUS result = game.run();

            GUIUtils.runPlatformLaterBlocking(() -> onGameEnded(result));
            onComplete.run();
        } catch (UncheckedInterruptedException _) {
        }

    }

    // GUI API: return the node to embed in a parent layout
    public Pane getPane() {
        return pane;
    }

    public int moveEvent(@SuppressWarnings("unused") PLAYER[][] state, int localBoardSel, int[] playableActions) {
        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Choose your cell!"));
        int action = globalBoard.selectMove(guiActor.getPlayer(), localBoardSel, playableActions, exit);
        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Game running..."));
        return action;
    }

    public int chooseBoardEvent(@SuppressWarnings("unused") PLAYER[][] state, int[] playableBoards) {
        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Choose your board!"));
        int action = globalBoard.chooseBoard(guiActor.getPlayer(), playableBoards, exit);
        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Game running..."));
        return action;
    }

    private Move[] getHistory() {
        if (moveHistory == null)
            return moveHistoryGenerator.getHistory();
        else
            return moveHistory;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasPrevious() {
        return currentStep > 0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasNext() {
        return currentStep < getHistory().length;
    }

    public void navigateHistory(int delta) {
        if (delta == -1 && currentStep == 1) {
            goToStart();
            return;
        }

        Move[] history = getHistory();

        if (delta == 1) {
            Move move = history[(currentStep + 1) - 1];
            state[move.board()][move.action()] = move.player();
        } else if (delta == -1) {
            Move move = history[currentStep - 1];
            state[move.board()][move.action()] = null;
        } else {
            throw new IllegalArgumentException("Delta must be either -1 or 1");
        }

        currentStep = currentStep + delta;

        Move move = history[currentStep - 1];

        updateStepLabel();
        globalBoard.displayState(state, move.board(), move.action());
    }

    public void goToStart() {
        currentStep = 0;
        state = new PLAYER[9][9];
        updateStepLabel();
        globalBoard.displayState(state, null, null);
    }

    public void goToEnd() {
        Move[] history = getHistory();
        Move last = history[history.length - 1];
        currentStep = history.length;
        state = StorageManager.movesToState(history);
        updateStepLabel();
        globalBoard.displayState(state, last.board(), last.action());
    }

    private void updateStepLabel() {
        int historySize = getHistory().length;
        if (currentStep == 0) {
            stepLabel.setText("Move: 0 / " + historySize + " | Player: /");
        } else {
            stepLabel.setText("Move: " + currentStep + " / " + historySize +
                    " | Player: " + getHistory()[currentStep - 1].player());
        }
    }

    private void onGameEnded(ENDED_STATUS result) {
        goToEnd();
        updateStepLabel();
        String winner = switch (result) {
            case X -> "Player X wins!";
            case O -> "Player O wins!";
            case TIE -> "Draw!";
        };
        statusLabel.setText("Game finished: " + winner + " (" + getHistory().length + " moves)");
    }

    public void load(String path) {
        if (ran)
            throw new RuntimeException("GameGUI can only load / run once per instance.");
        ran = true;
        moveHistory = StorageManager.load(path);
        onGameEnded(moveHistory[moveHistory.length - 1].globalEndedStatus());
    }

    public void save(String path) {
        StorageManager.save(getHistory(), path);
    }

    public void stop() {
        exit.set(true);
    }
}
