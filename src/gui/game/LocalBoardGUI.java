package gui.game;

import gui.utils.GUIUtils;
import helper.Utils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

import java.util.concurrent.atomic.AtomicReference;

public class LocalBoardGUI {
    private final GridPane pane;
    private final Label[] cells = new Label[9];
    private final Label overlayLabel;

    public LocalBoardGUI() {
        pane = new GridPane();
        pane.setHgap(2);
        pane.setVgap(2);
        pane.setStyle("-fx-background-color: #3c3f41; -fx-padding: 5;");

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Label cell = new Label("");
                cell.setMinSize(50, 50);
                cell.setMaxSize(50, 50);
                cell.setAlignment(Pos.CENTER);
                cell.setFont(Font.font("Arial", FontWeight.BOLD, 24));
                cell.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

                int local = row * 3 + col;
                cells[local] = cell;
                cell.setPickOnBounds(true);
                pane.add(cell, col, row);
            }
        }

        overlayLabel = new Label();
        overlayLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 96));
        overlayLabel.setMinSize(150, 150);
        overlayLabel.setAlignment(Pos.CENTER);
        overlayLabel.setMouseTransparent(true);
        GridPane.setRowSpan(overlayLabel, 3);
        GridPane.setColumnSpan(overlayLabel, 3);

        pane.getChildren().add(overlayLabel);
    }

    public GridPane getPane() {
        return pane;
    }

    private void setCellStyle(Label cell, PLAYER player, boolean highlightEnemyMove, PLAYER highlightSelectionPlayer, boolean opaque) {
        if (highlightEnemyMove && highlightSelectionPlayer != null) {
            throw new IllegalArgumentException("A cell cannot be highlighted for both enemy move and selection at the same time.");
        }
        String bg = "#4a4a4a";
        String text;
        if (player == PLAYER.X) {
            text = opaque ? "#ff6b6b80" : "#ff6b6b";
        } else if (player == PLAYER.O) {
            text = opaque ? "#4ecdc480" : "#4ecdc4";
        } else {
            text = opaque ? "#ffffff80" : "#ffffff";
        }
        String border = "";
        if (highlightEnemyMove) {
            border = "-fx-border-color: yellow; -fx-border-width: 2;";
            bg = "#666";
        }
        if (highlightSelectionPlayer != null) {
            border = "-fx-border-color: " + ((highlightSelectionPlayer == PLAYER.X) ? "#ff6b6b" : "#4ecdc4") + "; -fx-border-width: 2;";
            bg = "#666";
        }
        cell.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + text + ";" + border);
    }

    public void displayState(PLAYER[] state, Integer lastMoveCell) {
        for (int i = 0; i < 9; i++) {
            PLAYER player = state[i];
            boolean enemyHighlight = (lastMoveCell != null && lastMoveCell == i);

            if (enemyHighlight && player == null) {
                throw new IllegalArgumentException("Last move cell cannot be empty.");
            }

            Label cell = cells[i];
            if (player == PLAYER.X) {
                cell.setText("X");
                setCellStyle(cell, PLAYER.X, enemyHighlight, null, false);
            } else if (player == PLAYER.O) {
                cell.setText("O");
                setCellStyle(cell, PLAYER.O, enemyHighlight, null, false);
            } else {
                cell.setText("");
                setCellStyle(cell, null, false, null, false);
            }

            ENDED_STATUS status = Utils.localEnded(state);
            setEnded(status);
        }
    }

    private void setEnded(ENDED_STATUS status) {
        if (status == null) {
            overlayLabel.setVisible(false);
            return;
        }

        String symbol;
        String color;
        if (status == ENDED_STATUS.X) {
            symbol = "X";
            color = "#ff6b6bA0";
        } else if (status == ENDED_STATUS.O) {
            symbol = "O";
            color = "#4ecdc4A0";
        } else if (status == ENDED_STATUS.TIE) {
            symbol = "—";
            color = "#ffd166A0";
        } else {
            throw new RuntimeException("Unknown ended status: " + status);
        }

        overlayLabel.setVisible(true);
        overlayLabel.setText(symbol);
        overlayLabel.setStyle("-fx-text-fill: " + color + "; -fx-background-color: rgba(0,0,0,0.35); -fx-alignment: center;");
    }

    public int selectCell(PLAYER player, int[] playableSelections, AtomicReference<Boolean> exit) {
        String highlightStyle = "-fx-background-color: #666;  -fx-padding: 5; -fx-border-color: " + ((player == PLAYER.X) ? "#ff6b6b" : "#4ecdc4") + ";";
        GUIUtils.runPlatformLaterBlocking(() -> pane.setStyle(highlightStyle));

        AtomicReference<Integer> action = new AtomicReference<>();
        for (int sel : playableSelections) {
            Label cell = cells[sel];
            cell.setOnMouseClicked(_ -> action.set(sel));
            GUIUtils.runPlatformLaterBlocking(() -> setCellStyle(cell, null, false, player, false));

            // preview X/O on Hover
            cell.setOnMouseEntered(_ -> {
                if (cell.getText().isEmpty()) {
                    cell.setText(player == PLAYER.X ? "X" : "O");
                    setCellStyle(cell, player, false, player, true);
                }
            });
            cell.setOnMouseExited(_ -> {
                if (action.get() == null && (player == PLAYER.X && "X".equals(cell.getText()) || player == PLAYER.O && "O".equals(cell.getText()))) {
                    cell.setText("");
                    setCellStyle(cell, null, false, player, false);
                }
            });
        }

        while (action.get() == null) {
            if (exit.get()) throw new UncheckedInterruptedException();
            try {
                //noinspection BusyWait
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        String normalStyle = "-fx-background-color: #3c3f41; -fx-padding: 5;";
        GUIUtils.runPlatformLaterBlocking(() -> pane.setStyle(normalStyle));

        for (int sel : playableSelections) {
            Label cell = cells[sel];
            cell.setOnMouseClicked(null);
            cell.setOnMouseEntered(null);
            cell.setOnMouseExited(null);
            GUIUtils.runPlatformLaterBlocking(() -> setCellStyle(cell, null, false, null, false));
        }

        return action.get();
    }

    public void startChooseBoard(PLAYER player, Runnable onChoose) {
        String highlightStyle = "-fx-background-color: #666;  -fx-padding: 5; -fx-border-color: " + ((player == PLAYER.X) ? "#ff6b6b" : "#4ecdc4") + ";";
        GUIUtils.runPlatformLaterBlocking(() -> pane.setStyle(highlightStyle));

        for (Label l : cells) {
            l.setOnMouseClicked(_ -> onChoose.run());
        }
    }

    public void endChooseBoard() {
        String normalStyle = "-fx-background-color: #3c3f41; -fx-padding: 5;";
        GUIUtils.runPlatformLaterBlocking(() -> pane.setStyle(normalStyle));
    }
}
