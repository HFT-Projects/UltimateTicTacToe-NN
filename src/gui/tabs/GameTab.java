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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.prefs.Preferences;

public class GameTab extends Tab {
    private GameGUI game;

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

    private final MainWindow mainWindow;
    private final BorderPane root;
    private ComboBox<String> cbMode;
    private ComboBox<String> cbPlayer;
    private HBox nnSettingsBox;
    private HBox playerSelectionBox;
    private HBox dfsSettingsBox;
    private TextField epochTf;
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

        game = new GameGUI(nnProvider, nnParamsProvider);

        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2b2b2b;");

        root.setTop(createModeSelectionPanel());
        root.setCenter(game.getPane());
        root.setBottom(createControlPanel());

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
        cbMode.getSelectionModel().selectFirst();

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

        nnSettingsBox = new HBox(8, epochLb, epochTf);
        nnSettingsBox.setAlignment(Pos.CENTER);
        nnSettingsBox.setVisible(false);
        nnSettingsBox.setManaged(false);

        Label dfsLb = new Label("Algorithm strength multiplier:");
        dfsLb.setStyle("-fx-text-fill: white");
        dfsStrengthSlider = new Slider(1, 500, 1);
        dfsStrengthSlider.setValue(Integer.parseInt(prefs.get("dfs_strength", "1")));
        Label dfsStrengthLb = new Label(Integer.toString((int) dfsStrengthSlider.getValue()));
        dfsStrengthLb.setStyle("-fx-text-fill: white");
        dfsStrengthSlider.valueProperty().addListener((_, _, newVal) -> dfsStrengthLb.setText(Integer.toString(newVal.intValue())));

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

        int epochCount = 0;
        if (mode == GameGUI.GAME_MODE.NN_VS_NN || mode == GameGUI.GAME_MODE.NN_VS_ALGORITHM) {
            String epoch = epochTf.getText().trim();
            try {
                epochCount = Integer.parseInt(epoch);
                if (epochCount < 1)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Epoch Count");
                alert.setHeaderText("Epoch Count Must Be a Positive Integer");
                alert.setContentText("Please enter a valid positive integer for the epoch count.");
                alert.showAndWait();
                resetBoard();
                return;
            }
            prefs.put("nn_training_epoch_count", epoch);
        }

        GameGUI.HUMAN_PLAYER_SYMBOL humanPlayerSymbol = null;
        if (mode == GameGUI.GAME_MODE.HUMAN_VS_NN || mode == GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM) {
            humanPlayerSymbol = strToPlayer.get(cbPlayer.getValue());
            if (humanPlayerSymbol == null)
                throw new RuntimeException("Unknown human player symbol selected");
            // Save selected human player symbol to preferences
            prefs.put("human_player_symbol", playerToStr.get(humanPlayerSymbol));
        }

        prefs.put("dfs_strength", Integer.toString((int) dfsStrengthSlider.getValue()));

        try {
            game.run(mode, humanPlayerSymbol, epochCount, (int) dfsStrengthSlider.getValue(), this::updateNavButtons);
        } catch (NNNotProvidedException e) {
            showNNNotProvidedAlert();
            resetBoard();
        } catch (InvalidParametersException e) {
            showInvalidParametersAlert();
            resetBoard();
        }
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

    private void enableStartGameControls() {
        boolean viewMode = cbMode.getValue().equals(VIEW_MODE);
        boolean nnMode = cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_NN)) || cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM));
        boolean algoMode = cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.NN_VS_ALGORITHM)) || cbMode.getValue().equals(modeToStr.get(GameGUI.GAME_MODE.HUMAN_VS_ALGORITHM));

        if (viewMode) {
            loadGameBtn.setDisable(false);
        } else {
            runBtn.setDisable(false);
            saveBtn.setDisable(true);
        }
        if (nnMode) {
            epochTf.setDisable(false);
        }
        if (algoMode) {
            dfsStrengthSlider.setDisable(false);
        }
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
            saveBtn.setDisable(false);
        }
        if (nnMode) {
            epochTf.setDisable(true);
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
        game = new GameGUI(nnProvider, nnParamsProvider);
        root.setCenter(game.getPane());
        updateNavButtons();
        enableStartGameControls();
    }
}
