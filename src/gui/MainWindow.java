package gui;

import gui.tabs.GameTab;
import gui.tabs.NNTab;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

public class MainWindow extends Application {
    public static void run() {
        launch();
    }

    private Preferences prefs;
    private FileChooser fileChooser;
    private Stage primaryStage;

    public String selectFile(boolean save) {
        File file;
        if (!save)
            file = fileChooser.showOpenDialog(primaryStage);
        else
            file = fileChooser.showSaveDialog(primaryStage);

        if (file == null)
            return null;

        if (file.getParentFile() != null) {
            fileChooser.setInitialDirectory(file.getParentFile());
            prefs.put("last_directory", file.getParentFile().getAbsolutePath());
        }
        return file.getAbsolutePath();
    }

    @Override
    public void start(Stage primaryStage) {
        Preferences mainPreferences = prefs = Preferences.userRoot().node("/ultimate_tic_tac_toe_nn");
        Preferences preferencesNNTabPanel1 = Preferences.userRoot().node("/ultimate_tic_tac_toe_nn/nn_tab/panel_1");
        Preferences preferencesNNTabPanel2 = Preferences.userRoot().node("/ultimate_tic_tac_toe_nn/nn_tab/panel_2");
        Preferences preferencesGameTab = Preferences.userRoot().node("/ultimate_tic_tac_toe_nn/game_tab");

        this.primaryStage = primaryStage;

        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialDirectory(new File(mainPreferences.get("last_directory", System.getProperty("user.dir"))));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        NNTab nnTab = new NNTab(this, preferencesNNTabPanel1, preferencesNNTabPanel2);
        tabPane.getTabs().add(nnTab);

        GameTab gameTab = new GameTab(this, preferencesGameTab, nnTab::getNet, nnTab::getNNParameters);
        tabPane.getTabs().add(gameTab);

        Scene scene = new Scene(tabPane, 1000, 800);
        primaryStage.setTitle("Ultimate Tic Tac Toe");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
}

