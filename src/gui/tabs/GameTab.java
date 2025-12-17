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

import java.util.function.Function;
import java.util.prefs.Preferences;

public class GameTab extends Tab {
    private GameGUI game;

    private final MainWindow mainWindow;
    private final BorderPane root;
    private ToggleGroup modeGroup;
    private RadioButton viewGame, nnVsNnRadio, humanVsNnRadio;
    private ToggleGroup playerGroup;
    private RadioButton playerXRadio, playerORadio, playerRandomRadio;
    private HBox playerSelectionBox;
    HBox epochControls;
    private TextField epochTf;
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
        switch (savedMode) {
            case "view_game" -> modeGroup.selectToggle(viewGame);
            case "human_vs_nn" -> modeGroup.selectToggle(humanVsNnRadio);
            case "nn_vs_nn" -> modeGroup.selectToggle(nnVsNnRadio);
            default -> throw new RuntimeException("Unknown saved game mode: " + savedMode);
        }

        // Load saved human player symbol from preferences
        String savedPlayerSymbol = prefs.get("human_player_symbol", "random");
        switch (savedPlayerSymbol) {
            case "X" -> playerGroup.selectToggle(playerXRadio);
            case "O" -> playerGroup.selectToggle(playerORadio);
            case "random" -> playerGroup.selectToggle(playerRandomRadio);
            default -> throw new RuntimeException("Unknown saved human player symbol: " + savedPlayerSymbol);
        }

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

        modeGroup = new ToggleGroup();

        viewGame = new RadioButton("View Game");
        viewGame.setToggleGroup(modeGroup);
        viewGame.setTextFill(Color.WHITE);

        nnVsNnRadio = new RadioButton("NN vs NN");
        nnVsNnRadio.setToggleGroup(modeGroup);
        nnVsNnRadio.setTextFill(Color.WHITE);

        humanVsNnRadio = new RadioButton("Human vs NN");
        humanVsNnRadio.setToggleGroup(modeGroup);
        humanVsNnRadio.setTextFill(Color.WHITE);

        HBox modeBox = new HBox(15, modeLabel, viewGame, nnVsNnRadio, humanVsNnRadio);
        modeBox.setAlignment(Pos.CENTER);

        loadGameBtn = new Button("Load Game");
        loadGameBtn.setVisible(false);
        loadGameBtn.setManaged(false);
        loadGameBtn.setOnAction(_ -> {
            String path = mainWindow.selectFile(false);
            if (path == null)
                return;
            prefs.put("game_mode", "view_game");
            game.load(path);
            updateNavButtons();
            disableStartGameControls();
        });

        Label epochLb = new Label("Epoch Count:");
        epochLb.setStyle("-fx-text-fill: white");
        epochTf = new TextField("Epoch Count");
        epochTf.setText(prefs.get("nn_vs_nn_epoch_count", "1000"));

        epochControls = new HBox(8, epochLb, epochTf);
        epochControls.setAlignment(Pos.CENTER);
        epochControls.setVisible(false);
        epochControls.setManaged(false);

        Label playerLabel = new Label("Human plays as:");
        playerLabel.setTextFill(Color.WHITE);
        playerLabel.setFont(Font.font("Arial", 14));

        playerGroup = new ToggleGroup();

        playerXRadio = new RadioButton("X");
        playerXRadio.setToggleGroup(playerGroup);
        playerXRadio.setTextFill(Color.web("#ff6b6b"));

        playerORadio = new RadioButton("O");
        playerORadio.setToggleGroup(playerGroup);
        playerORadio.setTextFill(Color.web("#4ecdc4"));

        playerRandomRadio = new RadioButton("Random");
        playerRandomRadio.setToggleGroup(playerGroup);
        playerRandomRadio.setSelected(true);
        playerRandomRadio.setTextFill(Color.LIGHTGRAY);

        playerSelectionBox = new HBox(15, playerLabel, playerXRadio, playerORadio, playerRandomRadio);
        playerSelectionBox.setAlignment(Pos.CENTER);
        playerSelectionBox.setVisible(false);
        playerSelectionBox.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((_, _, newVal) -> {
            boolean viewMode = newVal == viewGame;
            boolean nnMode = newVal == nnVsNnRadio;
            boolean humanMode = newVal == humanVsNnRadio;

            runBtn.setDisable(viewMode);

            loadGameBtn.setVisible(viewMode);
            loadGameBtn.setManaged(viewMode);

            epochControls.setVisible(nnMode);
            epochControls.setManaged(nnMode);

            playerSelectionBox.setVisible(humanMode);
            playerSelectionBox.setManaged(humanMode);
        });

        modePanel.getChildren().addAll(modeBox, playerSelectionBox, loadGameBtn, epochControls);
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
        // Save selected mode to preferences
        String selectedMode;
        if (humanVsNnRadio.isSelected())
            selectedMode = "human_vs_nn";
        else if (nnVsNnRadio.isSelected())
            selectedMode = "nn_vs_nn";
        else if (viewGame.isSelected())
            throw new RuntimeException("Unexpected 'View Game' mode when starting a new game");
        else
            throw new RuntimeException("Unknown game mode selected");
        prefs.put("game_mode", selectedMode);

        // Save selected human player symbol to preferences
        String selectedPlayerSymbol;
        if (playerXRadio.isSelected())
            selectedPlayerSymbol = "X";
        else if (playerORadio.isSelected())
            selectedPlayerSymbol = "O";
        else if (playerRandomRadio.isSelected())
            selectedPlayerSymbol = "random";
        else
            throw new RuntimeException("Unknown human player symbol selected");
        prefs.put("human_player_symbol", selectedPlayerSymbol);

        disableStartGameControls();

        GameGUI.GAME_MODE mode = nnVsNnRadio.isSelected()
                ? GameGUI.GAME_MODE.NN_VS_NN
                : GameGUI.GAME_MODE.HUMAN_VS_NN;

        int epochCount = 0;
        if (mode == GameGUI.GAME_MODE.NN_VS_NN) {
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
            prefs.put("nn_vs_nn_epoch_count", epoch);
        }

        GameGUI.HUMAN_PLAYER_SYMBOL humanPlayerSymbol = null;
        if (mode == GameGUI.GAME_MODE.HUMAN_VS_NN)
            if (playerXRadio.isSelected()) {
                humanPlayerSymbol = GameGUI.HUMAN_PLAYER_SYMBOL.X;
            } else if (playerORadio.isSelected()) {
                humanPlayerSymbol = GameGUI.HUMAN_PLAYER_SYMBOL.O;
            } else {
                humanPlayerSymbol = GameGUI.HUMAN_PLAYER_SYMBOL.RANDOM;
            }

        try {
            game.run(mode, humanPlayerSymbol, epochCount, this::updateNavButtons);
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
        if (modeGroup.getSelectedToggle() == viewGame) {
            loadGameBtn.setDisable(false);
        } else {
            runBtn.setDisable(false);
            saveBtn.setDisable(true);
        }
        if (modeGroup.getSelectedToggle() == nnVsNnRadio) {
            epochTf.setDisable(false);
        }
        resetBtn.setDisable(true);
        viewGame.setDisable(false);
        nnVsNnRadio.setDisable(false);
        humanVsNnRadio.setDisable(false);
        playerXRadio.setDisable(false);
        playerORadio.setDisable(false);
        playerRandomRadio.setDisable(false);
    }

    private void disableStartGameControls() {
        if (modeGroup.getSelectedToggle() == viewGame) {
            loadGameBtn.setDisable(true);
        } else {
            runBtn.setDisable(true);
            saveBtn.setDisable(false);
        }
        if (modeGroup.getSelectedToggle() == nnVsNnRadio) {
            epochTf.setDisable(true);
        }
        resetBtn.setDisable(false);
        viewGame.setDisable(true);
        nnVsNnRadio.setDisable(true);
        humanVsNnRadio.setDisable(true);
        playerXRadio.setDisable(true);
        playerORadio.setDisable(true);
        playerRandomRadio.setDisable(true);
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
