package gui.tabs;

import gui.MainWindow;
import gui.game.GameGUI;
import gui.game.NNNotProvidedException;
import gui.game.UncheckedInterruptedException;
import gui.utils.GUIUtils;
import helper.Utils;
import javafx.application.Platform;
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
import java.util.function.BiFunction;
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
    private CheckBox cbUsePredictor;
    private ComboBox<String> cbPredictor;
    private HBox nnSettingsBox;
    private HBox playerSelectionBox;
    private HBox dfsSettingsBox;
    private TextField epochTf;
    private CheckBox cbSwapActors;
    private Slider dfsStrengthSlider;
    private Label trainingResultStatsLabel;
    private ProgressBar trainingProgressBar;
    private Label trainingProgressLabel;
    private Label trainingPercentLabel;
    private HBox trainingProgressBox;
    private Button loadGameBtn;
    private Button firstBtn, prevBtn, nextBtn, lastBtn, runBtn, saveBtn, resetBtn;

    private Thread gameThread = null;
    private final Preferences prefs;
    private final Function<Integer, FFN> nnProvider;
    private final Function<Integer, NNTab.NNParameters> nnParamsProvider;
    private boolean stopFlag = false;

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

        cbUsePredictor.setSelected(Boolean.parseBoolean(prefs.get("use_predictor", "false")));

        // Load saved predictor from preferences
        String savedPredictor = prefs.get("predictor", "Algorithm");
        if (!cbPredictor.getItems().contains(savedPredictor))
            System.out.println("Unknown predictor mode: " + savedPredictor);
        else
            cbPredictor.setValue(savedPredictor);

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

        cbUsePredictor = new CheckBox("Use predictor (white)");
        cbUsePredictor.setStyle("-fx-text-fill: white");
        cbUsePredictor.selectedProperty().addListener((_, _, newVal) -> {
            cbPredictor.setDisable(!newVal);

            String mode = cbMode.getValue();
            boolean algoMode = mode.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM)) || mode.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM));
            boolean humanMode = mode.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_NN)) || mode.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM));
            if (cbPredictor.getValue().equals("Algorithm") && newVal && humanMode || algoMode) {
                dfsSettingsBox.setVisible(true);
                dfsSettingsBox.setManaged(true);
            } else {
                dfsSettingsBox.setVisible(false);
                dfsSettingsBox.setManaged(false);
            }
        });

        cbPredictor = new ComboBox<>();
        cbPredictor.getItems().addAll("NN (2)", "Algorithm");
        cbPredictor.getSelectionModel().selectFirst();
        cbPredictor.setDisable(true);
        cbPredictor.valueProperty().addListener((_, _, newVal) -> {
            String mode = cbMode.getValue();
            boolean algoMode = mode.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM)) || mode.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM));
            boolean humanMode = mode.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_NN)) || mode.equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM));

            if (newVal.equals("Algorithm") && cbUsePredictor.isSelected() && humanMode || algoMode) {
                dfsSettingsBox.setVisible(true);
                dfsSettingsBox.setManaged(true);
            } else {
                dfsSettingsBox.setVisible(false);
                dfsSettingsBox.setManaged(false);
            }
        });

        playerSelectionBox = new HBox(15, playerLabel, cbPlayer, cbUsePredictor, cbPredictor);
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
            boolean trainingMode = newVal.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM)) || newVal.equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_NN));

            runBtn.setDisable(viewMode);

            loadGameBtn.setVisible(viewMode);
            loadGameBtn.setManaged(viewMode);

            nnSettingsBox.setVisible(nnMode);
            nnSettingsBox.setManaged(nnMode);

            playerSelectionBox.setVisible(humanMode);
            playerSelectionBox.setManaged(humanMode);

            boolean dfs = algoMode || (humanMode && cbUsePredictor.isSelected() && cbPredictor.getValue().equals("Algorithm"));
            dfsSettingsBox.setVisible(dfs);
            dfsSettingsBox.setManaged(dfs);

            trainingProgressBox.setVisible(trainingMode);
            trainingProgressBox.setManaged(trainingMode);

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

        trainingResultStatsLabel = new Label("");
        trainingResultStatsLabel.setStyle("-fx-text-fill: white");
        trainingResultStatsLabel.setVisible(false);
        trainingResultStatsLabel.setManaged(false);
        controls.getChildren().add(trainingResultStatsLabel);

        trainingProgressBar = new ProgressBar(0);
        trainingProgressBar.setPrefWidth(300);
        trainingProgressBar.setVisible(false);

        trainingProgressLabel = new Label("");
        trainingPercentLabel = new Label("");
        trainingProgressLabel.setStyle("-fx-text-fill: white; -fx-font-family: monospace;");
        trainingPercentLabel.setStyle("-fx-text-fill: white; -fx-font-family: monospace;");

        trainingProgressBox = new HBox(8, trainingProgressBar, trainingProgressLabel, trainingPercentLabel);
        trainingProgressBox.setAlignment(Pos.CENTER);
        trainingProgressBox.setVisible(false);
        trainingProgressBox.setManaged(false);

        controls.getChildren().add(trainingProgressBox);

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

        int epochCount = getEpochCount();

        BiFunction<PLAYER, Integer, NNActor> getActor1 = (p, ep) -> new NNActor(p, nn1, params1.trainer(), Utils.calculateAlpha(params1.alphaDecay(), params1.alpha(), epochCount, ep), params1.gamma(), Utils.calculateEpsilon(params1.epsilonDecay(), params1.epsilon(), ep));
        BiFunction<PLAYER, Integer, NNActor> getActor2 = (p, ep) -> new NNActor(p, nn2, params2.trainer(), Utils.calculateAlpha(params2.alphaDecay(), params2.alpha(), epochCount, ep), params2.gamma(), Utils.calculateEpsilon(params2.epsilonDecay(), params2.epsilon(), ep));

        boolean swapActors = cbSwapActors.isSelected();

        // Save to preferences
        prefs.put("nn_training_epoch_count", Integer.toString(epochCount));
        prefs.put("nn_swap_nets_between_epochs", Boolean.toString(swapActors));

        trainingProgressBar.setVisible(true);

        // Start game thread
        if (gameThread != null)
            throw new RuntimeException("Game thread is already running.");
        gameThread = new Thread(() -> {
            try {
                if (epochCount > 0) {
                    runTrainingGames(getActor1, getActor2, epochCount, swapActors);
                }
                game.run(getActor1.apply(PLAYER.X, epochCount), getActor2.apply(PLAYER.O, epochCount), null, null, this::gameFinishedEvent);
            } catch (UncheckedInterruptedException _) {
            } finally {
                stopFlag = false;
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
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

        Actor predictor = null;
        if (cbUsePredictor.isSelected()) {
            switch (cbPredictor.getValue()) {
                case "NN (2)" -> {
                    FFN nnPred = nnProvider.apply(1);
                    if (nnPred == null) {
                        throw new NNNotProvidedException(1, "Predictor NN (2) must be provided for Human vs NN mode.");
                    }
                    NNTab.NNParameters predParams = nnParamsProvider.apply(1);
                    predictor = new NNActor(getPlayer(), nnPred, predParams.trainer(), predParams.alpha(), predParams.gamma(), 0);
                }
                case "Algorithm" -> {
                    int dfsStrength = (int) dfsStrengthSlider.getValue();
                    predictor = new DFSActor(getPlayer(), dfsStrength);
                    prefs.put("dfs_strength", Integer.toString(dfsStrength));
                }
                default -> throw new RuntimeException("Unknown predictor selected");
            }
        }

        // Save to preferences
        prefs.put("use_predictor", Boolean.toString(cbUsePredictor.isSelected()));
        if (cbUsePredictor.isSelected())
            prefs.put("predictor", cbPredictor.getValue());

        // final predictor for lambda capture
        final Actor fPredictor = predictor;

        // Start game thread
        if (gameThread != null)
            throw new RuntimeException("Game thread is already running.");
        gameThread = new Thread(() -> {
            try {
                game.run(actor1, actor2, humanActor, fPredictor, this::gameFinishedEvent);
            } catch (UncheckedInterruptedException _) {
            } finally {
                stopFlag = false;
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void runNNvsAlgoGame() throws NNNotProvidedException, ExcHandled {
        FFN nn = nnProvider.apply(0);

        if (nn == null)
            throw new NNNotProvidedException(0, "NN must be provided for NN vs Algorithm mode.");

        NNTab.NNParameters params = nnParamsProvider.apply(0);
        int dfsStrength = (int) dfsStrengthSlider.getValue();

        int epochCount = getEpochCount();

        BiFunction<PLAYER, Integer, NNActor> getNNActor = (p, ep) -> new NNActor(p, nn, params.trainer(), Utils.calculateAlpha(params.alphaDecay(), params.alpha(), epochCount, ep), params.gamma(), Utils.calculateEpsilon(params.epsilonDecay(), params.epsilon(), ep));
        BiFunction<PLAYER, Integer, DFSActor> getDFSActor = (p, _) -> new DFSActor(p, dfsStrength);

        boolean swapActors = cbSwapActors.isSelected();

        // Save to preferences
        prefs.put("nn_training_epoch_count", Integer.toString(epochCount));
        prefs.put("nn_swap_nets_between_epochs", Boolean.toString(swapActors));
        prefs.put("dfs_strength", Integer.toString(dfsStrength));

        trainingProgressBar.setVisible(true);

        // Start game thread
        if (gameThread != null)
            throw new RuntimeException("Game thread is already running.");
        gameThread = new Thread(() -> {
            try {
                if (epochCount > 0) {
                    runTrainingGames(getNNActor, getDFSActor, epochCount, swapActors);
                }

                game.run(getNNActor.apply(PLAYER.X, epochCount), getDFSActor.apply(PLAYER.O, epochCount), null, null, this::gameFinishedEvent);
            } catch (UncheckedInterruptedException _) {
            } finally {
                stopFlag = false;
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void runHumanVsAlgoGame() throws NNNotProvidedException {
        int dfsStrength = (int) dfsStrengthSlider.getValue();

        PLAYER humanPlayer = getPlayer();
        PLAYER algoPlayer = (humanPlayer == PLAYER.X) ? PLAYER.O : PLAYER.X;

        GUIActor humanActor = new GUIActor(humanPlayer, game::moveEvent, game::chooseBoardEvent);
        DFSActor algoActor = new DFSActor(algoPlayer, dfsStrength);

        Actor actor1 = (humanPlayer == PLAYER.X) ? humanActor : algoActor;
        Actor actor2 = (humanPlayer == PLAYER.X) ? algoActor : humanActor;

        Actor predictor = null;
        if (cbUsePredictor.isSelected()) {
            switch (cbPredictor.getValue()) {
                case "NN (2)" -> {
                    FFN nnPred = nnProvider.apply(1);
                    if (nnPred == null) {
                        throw new NNNotProvidedException(1, "Predictor NN (2) must be provided for Human vs NN mode.");
                    }
                    NNTab.NNParameters predParams = nnParamsProvider.apply(1);
                    predictor = new NNActor(getPlayer(), nnPred, predParams.trainer(), predParams.alpha(), predParams.gamma(), 0);
                }
                case "Algorithm" -> {
                    predictor = new DFSActor(getPlayer(), dfsStrength);
                    prefs.put("dfs_strength", Integer.toString(dfsStrength));
                }
                default -> throw new RuntimeException("Unknown predictor selected");
            }
        }

        // Save to preferences
        prefs.put("dfs_strength", Integer.toString(dfsStrength));
        prefs.put("use_predictor", Boolean.toString(cbUsePredictor.isSelected()));
        if (cbUsePredictor.isSelected())
            prefs.put("predictor", cbPredictor.getValue());

        // final predictor for lambda capture
        final Actor fPredictor = predictor;

        // Start game thread
        if (gameThread != null)
            throw new RuntimeException("Game thread is already running.");
        gameThread = new Thread(() -> {
            try {
                game.run(actor1, actor2, humanActor, fPredictor, this::gameFinishedEvent);
            } catch (UncheckedInterruptedException _) {
            } finally {
                stopFlag = false;
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
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

    private void runTrainingGames(BiFunction<PLAYER, Integer, ? extends Actor> getActor1, BiFunction<PLAYER, Integer, ? extends Actor> getActor2, int count, boolean swapBetweenEpochs) {
        if (getActor1 == null || getActor2 == null)
            throw new RuntimeException("Actor providers cannot be null.");

        int ties = 0, xWon = 0, oWon = 0, actor1Won = 0, actor2Won = 0;
        final BiFunction<PLAYER, Integer, ? extends Actor> originalActor1 = getActor1;

        GUIUtils.runPlatformLaterBlocking(() -> {
            trainingResultStatsLabel.setVisible(true);
            trainingResultStatsLabel.setManaged(true);
        });

        for (int i = 0; i < count; i++) {
            if (stopFlag)
                throw new UncheckedInterruptedException();

            final int finalI = i;
            final int fTies = ties;
            final int fXWon = xWon;
            final int fOWon = oWon;
            final int fActor1Won = actor1Won;
            final int fActor2Won = actor2Won;
            Platform.runLater(() -> {
                trainingResultStatsLabel.setText("Results (ONLY Training): \nTies: " + fTies + "\nX Won: " + fXWon + "\nO Won: " + fOWon + "\nNN 1 Won: " + fActor1Won + "\nNN 2 Won: " + fActor2Won);
                trainingProgressBar.setProgress((double) finalI / count);
                int trainingProgressSize = Integer.toString(count).length();
                trainingProgressLabel.setText("Epoch " + String.format("%" + trainingProgressSize + "d", finalI) + "/" + count);
                trainingPercentLabel.setText(String.format("%6.2f%%", ((double) finalI / count) * 100));
            });

            if (swapBetweenEpochs && Math.random() > 0.5) {
                BiFunction<PLAYER, Integer, ? extends Actor> tmp = getActor1;
                getActor1 = getActor2;
                getActor2 = tmp;
            }
            Actor actorX = getActor1.apply(PLAYER.X, i);
            Actor actorO = getActor2.apply(PLAYER.O, i);
            Game game = new Game(actorX, actorO);
            if (actorX instanceof NNActor)
                game.addObserver(((NNActor) actorX)::eventHandler);
            if (actorO instanceof NNActor)
                game.addObserver(((NNActor) actorO)::eventHandler);
            switch (game.run()) {
                case TIE -> ties++;
                case X -> {
                    xWon++;
                    if (getActor1 == originalActor1)
                        actor1Won++;
                    else
                        actor2Won++;
                }
                case O -> {
                    oWon++;
                    if (getActor1 != originalActor1)
                        actor1Won++;
                    else
                        actor2Won++;
                }
            }
        }

        Platform.runLater(() -> {
            trainingProgressBar.setProgress(1);
            trainingProgressLabel.setText("Epoch " + count + "/" + count);
            trainingPercentLabel.setText("100,00%");
        });
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
        cbUsePredictor.setDisable(false);
        if (cbUsePredictor.isSelected())
            cbPredictor.setDisable(false);
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
        cbUsePredictor.setDisable(true);
        cbPredictor.setDisable(true);
    }

    private void updateNavButtons() {
        firstBtn.setDisable(!game.hasPrevious());
        prevBtn.setDisable(!game.hasPrevious());
        nextBtn.setDisable(!game.hasNext());
        lastBtn.setDisable(!game.hasNext());
    }

    private void resetBoard() {
        stopFlag = true;
        game.stop();

        try {
            if (gameThread != null)
                gameThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        stopFlag = false;

        gameThread = null;
        game = new GameGUI();
        root.setCenter(game.getPane());

        // use Platform.runLater to avoid JavaFX threading issues
        Platform.runLater(() -> {
            trainingProgressBar.setVisible(false);
            trainingProgressBar.setProgress(0);
            trainingProgressLabel.setText("");
            trainingPercentLabel.setText("");
            trainingResultStatsLabel.setVisible(false);
            trainingResultStatsLabel.setManaged(false);
        });

        updateNavButtons();
        enableStartGameControls();
    }
}
