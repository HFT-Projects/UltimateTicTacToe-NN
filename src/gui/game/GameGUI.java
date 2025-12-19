package gui.game;

import gui.tabs.NNTab.NNParameters;
import gui.utils.GUIUtils;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import nn.FFN;
import uttt.Game;
import uttt.actor.*;
import uttt.board.ENDED_STATUS;
import uttt.storage.Move;
import uttt.storage.MoveHistoryGenerator;
import uttt.storage.StorageManager;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

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

    private final Function<Integer, FFN> nnProvider;
    private final Function<Integer, NNParameters> nnParametersProvider;

    public GameGUI(Function<Integer, FFN> nnProvider, Function<Integer, NNParameters> nnParametersProvider) {
        this.nnProvider = nnProvider;
        this.nnParametersProvider = nnParametersProvider;

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

    // Run the underlying game and populate history (async)
    public void run(GAME_MODE mode, HUMAN_PLAYER_SYMBOL humanPlayerSymbolChoice, int trainingRunsCount, int dfsStrength, Runnable onComplete) throws NNNotProvidedException {
        if (ran)
            throw new RuntimeException("GameGUI can only run / load once per instance.");
        ran = true;

        NNParameters tParams1 = null;
        NNParameters tParams2 = null;
        NNParameters tParams = null;

        switch (mode) {
            case HUMAN_VS_NN:
                if (nnProvider.apply(0) == null)
                    throw new NNNotProvidedException(0, "NN 1 must be provided for HUMAN_VS_NN mode.");
                if (humanPlayerSymbolChoice == null)
                    throw new IllegalArgumentException("Human player symbol must be specified for HUMAN_VS_NN mode.");
                if (trainingRunsCount != 0)
                    throw new IllegalArgumentException("Training runs count must be zero for HUMAN_VS_NN mode.");
                tParams = nnParametersProvider.apply(0);
                break;
            case NN_VS_NN:
                if (nnProvider.apply(0) == null)
                    throw new NNNotProvidedException(0, "NN 1 must be provided for NN_VS_NN mode.");
                if (nnProvider.apply(1) == null)
                    throw new NNNotProvidedException(1, "NN 2 must be provided for NN_VS_NN mode.");
                if (humanPlayerSymbolChoice != null)
                    throw new IllegalArgumentException("Human player symbol must be null for NN_VS_NN mode.");
                if (trainingRunsCount < 0)
                    throw new IllegalArgumentException("Training runs count must be non-negative for NN_VS_NN mode.");
                tParams1 = nnParametersProvider.apply(0);
                tParams2 = nnParametersProvider.apply(1);
                break;
            case NN_VS_ALGORITHM:
                if (nnProvider.apply(0) == null)
                    throw new NNNotProvidedException(0, "NN 1 must be provided for NN_VS_ALGORITHM mode.");
                if (humanPlayerSymbolChoice != null)
                    throw new IllegalArgumentException("Human player symbol must be null for NN_VS_ALGORITHM mode.");
                if (trainingRunsCount < 0)
                    throw new IllegalArgumentException("Training runs count must be non-negative for NN_VS_ALGORITHM mode.");
                tParams1 = nnParametersProvider.apply(0);
                break;
            case HUMAN_VS_ALGORITHM:
                if (humanPlayerSymbolChoice == null)
                    throw new IllegalArgumentException("Human player symbol must be specified for HUMAN_VS_ALGORITHM mode.");
                if (trainingRunsCount != 0)
                    throw new IllegalArgumentException("Training runs count must be zero for HUMAN_VS_ALGORITHM mode.");
                break;
            default:
                throw new IllegalArgumentException("Unknown game mode: " + mode);
        }

        // for lambda capture
        final NNParameters fParams = tParams;
        final NNParameters fParams1 = tParams1;
        final NNParameters fParams2 = tParams2;

        Thread gameThread = new Thread(() -> {
            try {
                Actor actorX;
                Actor actorO;
                HUMAN_PLAYER_SYMBOL humanPlayerSymbol = humanPlayerSymbolChoice;

                switch (mode) {
                    case NN_VS_NN: {
                        FFN netX = nnProvider.apply(0);
                        FFN netO = nnProvider.apply(1);
                        Supplier<Actor> getActorX = () -> new NNActor(PLAYER.X, netX, fParams1.trainer(), fParams1.alpha(), fParams1.gamma(), fParams1.epsilon());
                        Supplier<Actor> getActorO = () -> new NNActor(PLAYER.O, netO, fParams2.trainer(), fParams2.alpha(), fParams2.gamma(), fParams2.epsilon());
                        actorX = getActorX.get();
                        actorO = getActorO.get();
                        runTrainingGames(getActorX, getActorO, trainingRunsCount);
                        break;
                    }
                    case HUMAN_VS_NN: {
                        if (humanPlayerSymbol == HUMAN_PLAYER_SYMBOL.RANDOM) {
                            humanPlayerSymbol = Math.random() < 0.5 ? HUMAN_PLAYER_SYMBOL.X : HUMAN_PLAYER_SYMBOL.O;
                        }
                        //noinspection EnhancedSwitchMigration
                        switch (humanPlayerSymbol) {
                            case X: {
                                actorX = guiActor = new GUIActor(PLAYER.X, this::moveEvent, this::chooseBoardEvent);
                                actorO = new NNActor(PLAYER.O, nnProvider.apply(0), fParams.trainer(), fParams.alpha(), fParams.gamma(), fParams.epsilon());
                                break;
                            }
                            case O: {
                                actorX = new NNActor(PLAYER.X, nnProvider.apply(0), fParams.trainer(), fParams.alpha(), fParams.gamma(), fParams.epsilon());
                                actorO = guiActor = new GUIActor(PLAYER.O, this::moveEvent, this::chooseBoardEvent);
                                break;
                            }
                            default:
                                throw new IllegalArgumentException("Unknown human player symbol: " + humanPlayerSymbol);
                        }
                        break;
                    }
                    case NN_VS_ALGORITHM: {
                        FFN netX = nnProvider.apply(0);
                        Supplier<Actor> getActorX = () -> new NNActor(PLAYER.X, netX, fParams1.trainer(), fParams1.alpha(), fParams1.gamma(), fParams1.epsilon());
                        actorX = getActorX.get();
                        Supplier<Actor> getActorO = () -> new DFSActor(PLAYER.O, dfsStrength);
                        actorO = getActorO.get();
                        runTrainingGames(getActorX, getActorO, trainingRunsCount);
                        break;
                    }
                    case HUMAN_VS_ALGORITHM: {
                        if (humanPlayerSymbol == HUMAN_PLAYER_SYMBOL.RANDOM) {
                            humanPlayerSymbol = Math.random() < 0.5 ? HUMAN_PLAYER_SYMBOL.X : HUMAN_PLAYER_SYMBOL.O;
                        }
                        //noinspection EnhancedSwitchMigration
                        switch (humanPlayerSymbol) {
                            case X: {
                                actorX = guiActor = new GUIActor(PLAYER.X, this::moveEvent, this::chooseBoardEvent);
                                actorO = new DFSActor(PLAYER.O, dfsStrength);
                                break;
                            }
                            case O: {
                                actorX = new DFSActor(PLAYER.X, dfsStrength);
                                actorO = guiActor = new GUIActor(PLAYER.O, this::moveEvent, this::chooseBoardEvent);
                                break;
                            }
                            default:
                                throw new IllegalArgumentException("Unknown human player symbol: " + humanPlayerSymbol);
                        }
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown game mode: " + mode);
                }

                uttt.Game game = new uttt.Game(actorX, actorO);
                if (actorX instanceof NNActor)
                    game.addObserver(((NNActor) actorX)::eventHandler);
                if (actorO instanceof NNActor)
                    game.addObserver(((NNActor) actorO)::eventHandler);

                game.addObserver(e -> {
                    moveHistoryGenerator.handleEvent(e);
                    if (mode == GAME_MODE.HUMAN_VS_NN || mode == GAME_MODE.HUMAN_VS_ALGORITHM)
                        Platform.runLater(this::goToEnd);
                });

                switch (mode) {
                    case HUMAN_VS_ALGORITHM:
                    case HUMAN_VS_NN:
                        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Game running..."));
                        break;
                    case NN_VS_ALGORITHM:
                    case NN_VS_NN:
                        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("Game running..."));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown game mode: " + mode);
                }

                // run the game loop
                ENDED_STATUS result = game.run();

                GUIUtils.runPlatformLaterBlocking(() -> onGameEnded(result));
                onComplete.run();
            } catch (UncheckedInterruptedException _) {
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }

    // GUI API: return the node to embed in a parent layout
    public Pane getPane() {
        return pane;
    }

    private void runTrainingGames(Supplier<Actor> getActorX, Supplier<Actor> getActorO, int count) {
        if (getActorX == null || getActorO == null)
            throw new RuntimeException("Actor providers cannot be null.");

        for (int i = 0; i < count; i++) {
            if (Math.random() > 0.5) {
                Supplier<Actor> tmp = getActorX;
                getActorX = getActorO;
                getActorO = tmp;
            }
            Actor actorX = getActorX.get();
            Actor actorO = getActorO.get();
            Game game = new Game(actorX, actorO);
            if (actorX instanceof NNActor)
                game.addObserver(((NNActor) actorX)::eventHandler);
            if (actorO instanceof NNActor)
                game.addObserver(((NNActor) actorO)::eventHandler);
            game.run();
        }
    }

    private int moveEvent(PLAYER[][] state, int localBoardSel, int[] playableActions) {
        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Choose your cell!"));
        int action = globalBoard.selectMove(guiActor.getPlayer(), localBoardSel, playableActions, exit);
        GUIUtils.runPlatformLaterBlocking(() -> statusLabel.setText("You play as " + guiActor.getPlayer() + " - Game running..."));
        return action;
    }

    private int chooseBoardEvent(PLAYER[][] state, int[] playableBoards) {
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
