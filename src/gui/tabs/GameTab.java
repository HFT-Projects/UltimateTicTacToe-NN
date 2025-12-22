package gui.tabs;

import gui.MainWindow;
import gui.game.GameGUI;
import gui.game.NNNotProvidedException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import nn.FFN;
import org.jspecify.annotations.NonNull;
import uttt.Game;
import uttt.actor.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.prefs.Preferences;

public class GameTab extends Tab {
    private static class ExcHandled extends Exception {
    }

    private static final String VIEW_MODE = "View Game";
    private static final Map<GameGUI.GAME_MODE, String> modeToStr;
    private static final Map<String, GameGUI.GAME_MODE> strToMode;
    private static final Map<GameGUI.HUMAN_PLAYER_SYMBOL, String> playerToStr;
    private static final Map<String, GameGUI.HUMAN_PLAYER_SYMBOL> strToPlayer;

    static {
        Map<GameGUI.GAME_MODE, String> tmpModeToStr = new LinkedHashMap<>();
        tmpModeToStr.put(GameGUI.GAME_MODE.NN_VS_NN, "NN vs NN");
        tmpModeToStr.put(GameGUI.GAME_MODE.HUMAN_VS_NN, "Human vs NN");
        tmpModeToStr.put(GameGUI.GAME_MODE.NN_VS_ALGORITHM, "NN vs Algorithm");
        tmpModeToStr.put(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM, "Human vs Algorithm");
        modeToStr = Collections.unmodifiableMap(tmpModeToStr);

        Map<String, GameGUI.GAME_MODE> tmpStrToMode = new LinkedHashMap<>();
        for (Map.Entry<GameGUI.GAME_MODE, String> entry : tmpModeToStr.entrySet()) {
            tmpStrToMode.put(entry.getValue(), entry.getKey());
        }
        strToMode = Collections.unmodifiableMap(tmpStrToMode);

        Map<GameGUI.HUMAN_PLAYER_SYMBOL, String> tmpPlayerToStr = new LinkedHashMap<>();
        tmpPlayerToStr.put(GameGUI.HUMAN_PLAYER_SYMBOL.X, "X");
        tmpPlayerToStr.put(GameGUI.HUMAN_PLAYER_SYMBOL.O, "O");
        tmpPlayerToStr.put(GameGUI.HUMAN_PLAYER_SYMBOL.RANDOM, "Random");
        playerToStr = Collections.unmodifiableMap(tmpPlayerToStr);

        Map<String, GameGUI.HUMAN_PLAYER_SYMBOL> tmpStrToPlayer = new LinkedHashMap<>();
        for (Map.Entry<GameGUI.HUMAN_PLAYER_SYMBOL, String> entry : tmpPlayerToStr.entrySet()) {
            tmpStrToPlayer.put(entry.getValue(), entry.getKey());
        }
        strToPlayer = Collections.unmodifiableMap(tmpStrToPlayer);
    }

    private GameGUI game;

    private final MainWindow mainWindow;
    private final BorderPane root;
    private ComboBox<String> cbMode;
    private ComboBox<String> cbPlayer;
    private HBox nnSettingsBox;
    private HBox playerSelectionBox;
    private HBox dfsSettingsBox;
    private TextField epochTf;
    private CheckBox cbSwapActors;
    private Slider dfsStrengthSlider;
    private Button loadGameBtn;
    private Button firstBtn, prevBtn, nextBtn, lastBtn, runBtn, saveBtn, resetBtn;

    private final Preferences prefs;
    private final Function<Integer, FFN> nnProvider;
    private final Function<Integer, NNTab.NNParameters> nnParamsProvider;

    public GameTab(MainWindow mainWindow, Preferences prefs, Function<Integer, FFN> nnProvider, Function<Integer, NNTab.NNParameters> nnParamsProvider) {
        this.mainWindow = mainWindow;
        this.prefs = prefs;
        this.nnProvider = nnProvider;
        this.nnParamsProvider = nnParamsProvider;

        setText("Game");

        game = new GameGUI();

        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2b2b2b;");

        root.setTop(createModeSelectionPanel());
        root.setCenter(game.getPane());
        root.setBottom(createControlPanel());

        // needs to be initialized after creation of control panel
        cbMode.getSelectionModel().selectFirst();

        // Load saved mode from preferences
        String savedMode = prefs.get("game_mode", "nn_vs_nn");
        if (!cbMode.getItems().contains(savedMode))
            System.out.println("Unknown saved game mode: " + savedMode);
        else
            cbMode.setValue(savedMode);

        // Load saved human player symbol from preferences
        String savedPlayerSymbol = prefs.get("human_player_symbol", "random");
        if (!cbPlayer.getItems().contains(savedPlayerSymbol))
            System.out.println("Unknown saved human player symbol: " + savedPlayerSymbol);
        else
            cbPlayer.setValue(savedPlayerSymbol);

        setContent(root);

        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.LEFT) {
                if (!prevBtn.isDisabled()) {
                    game.navigateHistory(-1);
                    updateNavButtons();
                }
            } else if (event.getCode() == KeyCode.RIGHT) {
                if (!nextBtn.isDisabled()) {
                    game.navigateHistory(1);
                    updateNavButtons();
                }
            }
        });
    }

    private VBox createModeSelectionPanel() {
        VBox modePanel = new VBox(10);
        modePanel.setAlignment(Pos.CENTER);
        modePanel.setPadding(new Insets(0, 0, 15, 0));

        Label modeLabel = new Label("Game mode:");
        modeLabel.setTextFill(Color.WHITE);
        modeLabel.setFont(Font.font("Arial", 14));

        cbMode = new ComboBox<>();
        cbMode.getItems().add(VIEW_MODE);
        cbMode.getItems().addAll(strToMode.keySet());

        loadGameBtn = new Button("Load Game");
        loadGameBtn.setVisible(false);
        loadGameBtn.setManaged(false);
        loadGameBtn.setOnAction(_ -> {
            String path = mainWindow.selectFile(false);
            if (path == null)
                return;
            prefs.put("game_mode", VIEW_MODE);
            game.load(path);
            updateNavButtons();
            disableStartGameControls();
        });

        Label epochLb = new Label("Epoch Count:");
        epochLb.setStyle("-fx-text-fill: white");
        epochTf = new TextField("Epoch Count");
        epochTf.setText(prefs.get("nn_training_epoch_count", "1000"));

        cbSwapActors = new CheckBox("Swap actors randomly between epochs");
        cbSwapActors.setStyle("-fx-text-fill: white");
        cbSwapActors.setSelected(Boolean.parseBoolean(prefs.get("nn_swap_nets_between_epochs", "true")));

        nnSettingsBox = new HBox(8, epochLb, epochTf, cbSwapActors);
        nnSettingsBox.setAlignment(Pos.CENTER);
        nnSettingsBox.setVisible(false);
        nnSettingsBox.setManaged(false);

        Label dfsLb = new Label("Algorithm strength multiplier:");
        dfsLb.setStyle("-fx-text-fill: white");
        dfsStrengthSlider = new Slider(1, 500, 1);
        Label dfsStrengthLb = new Label();
        dfsStrengthLb.setStyle("-fx-text-fill: white");
        dfsStrengthSlider.valueProperty().addListener((_, _, newVal) -> dfsStrengthLb.setText(Integer.toString(newVal.intValue())));
        dfsStrengthSlider.setValue(Integer.parseInt(prefs.get("dfs_strength", "1")));

        dfsSettingsBox = new HBox(8, dfsLb, dfsStrengthSlider, dfsStrengthLb);
        dfsSettingsBox.setAlignment(Pos.CENTER);
        dfsSettingsBox.setVisible(false);
        dfsSettingsBox.setManaged(false);

        Label playerLabel = new Label("Human plays as:");
        playerLabel.setTextFill(Color.WHITE);
        playerLabel.setFont(Font.font("Arial", 14));

        cbPlayer = new ComboBox<>();
        cbPlayer.getItems().addAll(strToPlayer.keySet());
        cbPlayer.getSelectionModel().selectFirst();

        playerSelectionBox = new HBox(15, playerLabel, cbPlayer);
        playerSelectionBox.setAlignment(Pos.CENTER);
        playerSelectionBox.setVisible(false);
        playerSelectionBox.setManaged(false);

        HBox settingsBox = new HBox(8, playerSelectionBox, loadGameBtn, nnSettingsBox, dfsSettingsBox);
        settingsBox.setAlignment(Pos.CENTER);

        cbMode.valueProperty().addListener((_, _, newVal) -> {
            boolean viewMode = newVal.equals(VIEW_MODE);
            boolean nnMode = newVal.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_NN)) || newVal.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM));
            boolean humanMode = newVal.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_NN)) || newVal.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM));
            boolean algoMode = newVal.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM)) || newVal.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM));

            runBtn.setDisable(viewMode);

            loadGameBtn.setVisible(viewMode);
            loadGameBtn.setManaged(viewMode);

            nnSettingsBox.setVisible(nnMode);
            nnSettingsBox.setManaged(nnMode);

            playerSelectionBox.setVisible(humanMode);
            playerSelectionBox.setManaged(humanMode);

            dfsSettingsBox.setVisible(algoMode);
            dfsSettingsBox.setManaged(algoMode);

            // update slider max
            if (newVal.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM))) {
                dfsStrengthSlider.setMax(30);
            } else if (newVal.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM))) {
                dfsStrengthSlider.setMax(500);
            }
        });

        modePanel.getChildren().addAll(cbMode, settingsBox);
        return modePanel;
    }

    private VBox createControlPanel() {
        VBox controls = new VBox(10);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20, 0, 0, 0));

        HBox navButtons = new HBox(10);
        navButtons.setAlignment(Pos.CENTER);

        firstBtn = new Button("⏮");
        firstBtn.setDisable(true);
        firstBtn.setOnAction(_ -> {
            game.goToStart();
            updateNavButtons();
        });

        prevBtn = new Button("◀");
        prevBtn.setDisable(true);
        prevBtn.setOnAction(_ -> {
            game.navigateHistory(-1);
            updateNavButtons();
        });

        nextBtn = new Button("▶");
        nextBtn.setDisable(true);
        nextBtn.setOnAction(_ -> {
            game.navigateHistory(1);
            updateNavButtons();
        });

        lastBtn = new Button("⏭");
        lastBtn.setDisable(true);
        lastBtn.setOnAction(_ -> {
            game.goToEnd();
            updateNavButtons();
        });

        runBtn = new Button("Start Game");
        runBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        runBtn.setOnAction(_ -> runGame());

        saveBtn = new Button("Save Game");
        saveBtn.setDisable(true);
        saveBtn.setOnAction(_ -> {
            String path = mainWindow.selectFile(true);
            if (path == null)
                return;
            game.save(path);
        });

        resetBtn = new Button("Reset");
        resetBtn.setDisable(true);
        resetBtn.setOnAction(_ -> resetBoard());

        navButtons.getChildren().addAll(firstBtn, prevBtn, runBtn, nextBtn, lastBtn, saveBtn, resetBtn);
        controls.getChildren().addAll(navButtons);

        return controls;
    }

    private void runGame() {
        GameGUI.GAME_MODE mode = strToMode.get(cbMode.getValue());
        if (mode == null) {
            throw new RuntimeException("Unknown game mode selected");
        }

        // Save selected mode to preferences
        prefs.put("game_mode", modeToStr.get(mode));

        disableStartGameControls();

        try {
            switch (mode) {
                case NN_VS_NN -> runNNvsNNGame();
                case HUMAN_VS_NN -> runHumanVsNNGame();
                case NN_VS_ALGORITHM -> runNNvsAlgoGame();
                case HUMAN_VS_ALGORITHM -> runHumanVsAlgoGame();
                default -> throw new RuntimeException("Unknown game mode: " + mode);
            }
        } catch (NNNotProvidedException e) {
            showNNNotProvidedAlert();
            resetBoard();
        } catch (InvalidParametersException e) {
            showInvalidParametersAlert();
            resetBoard();
        } catch (ExcHandled _) {
            // already handled
            resetBoard();
        }
    }

    private int getEpochCount() throws ExcHandled {
        String epoch = epochTf.getText().trim();
        try {
            int epochCount = Integer.parseInt(epoch);
            if (epochCount < 1)
                throw new NumberFormatException();
            return epochCount;
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Epoch Count");
            alert.setHeaderText("Epoch Count Must Be a Positive Integer");
            alert.setContentText("Please enter a valid positive integer for the epoch count.");
            alert.showAndWait();
            resetBoard();
            throw new ExcHandled();
        }
    }

    private @NonNull PLAYER getPlayer() {
        GameGUI.HUMAN_PLAYER_SYMBOL humanPlayerSymbol = strToPlayer.get(cbPlayer.getValue());
        if (humanPlayerSymbol == null)
            throw new RuntimeException("Unknown human player symbol selected");

        // Save to preferences
        prefs.put("human_player_symbol", playerToStr.get(humanPlayerSymbol));

        return switch (humanPlayerSymbol) {
            case GameGUI.HUMAN_PLAYER_SYMBOL.X -> PLAYER.X;
            case GameGUI.HUMAN_PLAYER_SYMBOL.O -> PLAYER.O;
            case GameGUI.HUMAN_PLAYER_SYMBOL.RANDOM -> Math.random() < 0.5 ? PLAYER.X : PLAYER.O;
        };
    }

    private void runNNvsNNGame() throws NNNotProvidedException, ExcHandled {
        FFN nn1 = nnProvider.apply(0);
        FFN nn2 = nnProvider.apply(1);

        if (nn1 == null)
            throw new NNNotProvidedException(0, "NN 1 must be provided for NN vs NN mode.");
        if (nn2 == null)
            throw new NNNotProvidedException(1, "NN 2 must be provided for NN vs NN mode.");

        NNTab.NNParameters params1 = nnParamsProvider.apply(0);
        NNTab.NNParameters params2 = nnParamsProvider.apply(1);

        Function<PLAYER, NNActor> getActor1 = p -> new NNActor(p, nn1, params1.trainer(), params1.alpha(), params1.gamma(), params1.epsilon());
        Function<PLAYER, NNActor> getActor2 = p -> new NNActor(p, nn2, params2.trainer(), params2.alpha(), params2.gamma(), params2.epsilon());

        boolean swapActors = cbSwapActors.isSelected();

        int epochCount = getEpochCount();
        if (epochCount > 0)
            runTrainingGames(getActor1, getActor2, epochCount, swapActors);

        // Save to preferences
        prefs.put("nn_training_epoch_count", Integer.toString(epochCount));
        prefs.put("nn_swap_nets_between_epochs", Boolean.toString(swapActors));

        game.run(getActor1.apply(PLAYER.X), getActor2.apply(PLAYER.O), null, this::gameFinishedEvent);
    }

    private void runHumanVsNNGame() throws NNNotProvidedException {
        FFN nn = nnProvider.apply(0);

        if (nn == null)
            throw new NNNotProvidedException(0, "NN must be provided for Human vs NN mode.");

        NNTab.NNParameters params = nnParamsProvider.apply(0);

        PLAYER humanPlayer = getPlayer();
        PLAYER nnPlayer = (humanPlayer == PLAYER.X) ? PLAYER.O : PLAYER.X;

        GUIActor humanActor = new GUIActor(humanPlayer, game::moveEvent, game::chooseBoardEvent);
        NNActor nnActor = new NNActor(nnPlayer, nn, params.trainer(), params.alpha(), params.gamma(), params.epsilon());

        Actor actor1 = (humanPlayer == PLAYER.X) ? humanActor : nnActor;
        Actor actor2 = (humanPlayer == PLAYER.X) ? nnActor : humanActor;

        game.run(actor1, actor2, humanActor, this::gameFinishedEvent);
    }

    private void runNNvsAlgoGame() throws NNNotProvidedException, ExcHandled {
        FFN nn = nnProvider.apply(0);

        if (nn == null)
            throw new NNNotProvidedException(0, "NN must be provided for NN vs Algorithm mode.");

        NNTab.NNParameters params = nnParamsProvider.apply(0);
        int dfsStrength = (int) dfsStrengthSlider.getValue();

        Function<PLAYER, NNActor> getNNActor = p -> new NNActor(p, nn, params.trainer(), params.alpha(), params.gamma(), params.epsilon());
        Function<PLAYER, DFSActor> getDFSActor = p -> new DFSActor(p, dfsStrength);

        boolean swapActors = cbSwapActors.isSelected();

        int epochCount = getEpochCount();
        if (epochCount > 0)
            runTrainingGames(getNNActor, getDFSActor, epochCount, swapActors);

        // Save to preferences
        prefs.put("nn_training_epoch_count", Integer.toString(epochCount));
        prefs.put("nn_swap_nets_between_epochs", Boolean.toString(swapActors));
        prefs.put("dfs_strength", Integer.toString(dfsStrength));

        game.run(getNNActor.apply(PLAYER.X), getDFSActor.apply(PLAYER.O), null, this::gameFinishedEvent);
    }

    private void runHumanVsAlgoGame() throws NNNotProvidedException {
        int dfsStrength = (int) dfsStrengthSlider.getValue();

        PLAYER humanPlayer = getPlayer();
        PLAYER algoPlayer = (humanPlayer == PLAYER.X) ? PLAYER.O : PLAYER.X;

        GUIActor humanActor = new GUIActor(humanPlayer, game::moveEvent, game::chooseBoardEvent);
        DFSActor algoActor = new DFSActor(algoPlayer, dfsStrength);

        Actor actor1 = (humanPlayer == PLAYER.X) ? humanActor : algoActor;
        Actor actor2 = (humanPlayer == PLAYER.X) ? algoActor : humanActor;

        // Save to preferences
        prefs.put("dfs_strength", Integer.toString(dfsStrength));

        game.run(actor1, actor2, humanActor, this::gameFinishedEvent);
    }

    private void showNNNotProvidedAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Neural Network Not Provided");
        alert.setContentText("One or more neural networks required for this game mode are not provided. Please create the neural networks in the 'Neural Networks' tab before starting the game.");
        alert.showAndWait();
    }

    private void showInvalidParametersAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Invalid Neural Network Parameters");
        alert.setContentText("The parameters for one or more neural networks are invalid. Please check the 'Neural Networks' tab and ensure all parameters are correctly set.");
        alert.showAndWait();
    }

    private void runTrainingGames(Function<PLAYER, ? extends Actor> getActor1, Function<PLAYER, ? extends Actor> getActor2, int count, boolean swapBetweenEpochs) {
        if (getActor1 == null || getActor2 == null)
            throw new RuntimeException("Actor providers cannot be null.");

        for (int i = 0; i < count; i++) {
            if (swapBetweenEpochs && Math.random() > 0.5) {
                Function<PLAYER, ? extends Actor> tmp = getActor1;
                getActor1 = getActor2;
                getActor2 = tmp;
            }
            Actor actorX = getActor1.apply(PLAYER.X);
            Actor actorO = getActor2.apply(PLAYER.O);
            Game game = new Game(actorX, actorO);
            if (actorX instanceof NNActor)
                game.addObserver(((NNActor) actorX)::eventHandler);
            if (actorO instanceof NNActor)
                game.addObserver(((NNActor) actorO)::eventHandler);
            game.run();
        }
    }

    private void gameFinishedEvent() {
        updateNavButtons();
        saveBtn.setDisable(false);
    }

    private void enableStartGameControls() {
        boolean viewMode = cbMode.getValue().equals(VIEW_MODE);
        boolean nnMode = cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_NN)) || cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM));
        boolean algoMode = cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM)) || cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM));

        if (viewMode) {
            loadGameBtn.setDisable(false);
        } else {
            runBtn.setDisable(false);
        }
        if (nnMode) {
            epochTf.setDisable(false);
            cbSwapActors.setDisable(false);
        }
        if (algoMode) {
            dfsStrengthSlider.setDisable(false);
        }
        saveBtn.setDisable(true);
        resetBtn.setDisable(true);
        cbMode.setDisable(false);
        cbPlayer.setDisable(false);
    }

    private void disableStartGameControls() {
        @SuppressWarnings("DuplicatedCode")
        boolean viewMode = cbMode.getValue().equals(VIEW_MODE);
        boolean nnMode = cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_NN)) || cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM));
        boolean algoMode = cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM)) || cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM));

        if (viewMode) {
            loadGameBtn.setDisable(true);
        } else {
            runBtn.setDisable(true);
        }
        if (nnMode) {
            epochTf.setDisable(true);
            cbSwapActors.setDisable(true);
        }
        if (algoMode) {
            dfsStrengthSlider.setDisable(true);
        }
        resetBtn.setDisable(false);
        cbMode.setDisable(true);
        cbPlayer.setDisable(true);
    }

    private void updateNavButtons() {
        firstBtn.setDisable(!game.hasPrevious());
        prevBtn.setDisable(!game.hasPrevious());
        nextBtn.setDisable(!game.hasNext());
        lastBtn.setDisable(!game.hasNext());
    }

    private void resetBoard() {
        game.stop();
        game = new GameGUI();
        root.setCenter(game.getPane());
        updateNavButtons();
        enableStartGameControls();
    }
}
