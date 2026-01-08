package gui.tabs;

import gui.MainWindow;
import helper.Utils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import nn.FFN;
import nn.activation.*;
import nn.loss.LossFunction;
import nn.loss.MeanSquaredError;
import nn.trainer.FFNTrainer;
import nn.trainer.FFNTrainerBGD;
import nn.trainer.FFNTrainerMBGD;
import nn.trainer.FFNTrainerSGD;

import java.util.*;
import java.util.prefs.Preferences;

public class NNTab extends Tab {
    private class NNPanel {
        private final Preferences prefs;
        private FFN net;

        private final String title;
        private final VBox box;
        private final TextField tfHidden;
        private final TextField tfSelectionMultiplier;
        private final ComboBox<String> cbAct;
        private final ComboBox<String> cbLoss;
        private final Button btnCreate;
        private final Button btnImport;
        private final Button btnExport;
        private final Button btnDelete;
        private final TextField tfAlpha;
        private final CheckBox cbAlphaDecay;
        private final TextField tfGamma;
        private final TextField tfEpsilon;
        private final CheckBox cbEpsilonDecay;
        private final ComboBox<String> cbTrainer;
        private final TextField tfBatchSize;

        public NNPanel(String title, Preferences prefs) {
            this.prefs = prefs;
            this.title = title;

            box = new VBox(10);
            box.setPadding(new Insets(12));
            box.setAlignment(Pos.TOP_LEFT);

            Label lblTitle = new Label(title);
            lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            // Hidden layer sizes input (comma separated)
            Label lblHidden = new Label("Hidden layer sizes (comma separated, e.g. 64,32):");
            tfHidden = new TextField();

            // Hidden layer sizes input (comma separated)
            Label lblSelectionMultiplier = new Label("Board selection multiplier (int):");
            tfSelectionMultiplier = new TextField();

            // Activation function for hidden layers
            Label lblAct = new Label("Activation function (hidden):");
            cbAct = new ComboBox<>();
            cbAct.getItems().addAll(Utils.nameToActivation.keySet());
            cbAct.getSelectionModel().selectFirst();

            // Loss function
            Label lblLoss = new Label("Loss function:");
            cbLoss = new ComboBox<>();
            cbLoss.getItems().addAll(Utils.nameToLoss.keySet());
            cbLoss.getSelectionModel().selectFirst();

            // Error / info labels
            Label lblError = new Label();
            lblError.setStyle("-fx-text-fill: red;");

            Label lblSummary = new Label("Summary: -");
            lblSummary.setWrapText(true);

            btnCreate = new Button("Create NN");
            btnImport = new Button("Import NN");
            btnExport = new Button("Export NN");
            btnDelete = new Button("Delete NN");

            btnCreate.setOnAction(_ -> {
                lblError.setText("");
                List<Integer> hiddenSizes;
                try {
                    hiddenSizes = parseHiddenSizes(tfHidden.getText());
                    if (hiddenSizes.isEmpty()) {
                        lblError.setText("At least one hidden layer must be specified.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    lblError.setText("Invalid hidden layer input. Only positive integers, comma separated.");
                    return;
                }

                int selectionMultiplier;
                try {
                    selectionMultiplier = Integer.parseInt(tfSelectionMultiplier.getText().trim());
                    if (selectionMultiplier <= 0) {
                        lblError.setText("Board selection multiplier must be a positive integer.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    lblError.setText("Invalid board selection multiplier. Must be a positive integer.");
                    return;
                }

                String act = cbAct.getValue();
                String loss = cbLoss.getValue();

                final int inputSize = 18 * 9 + 9 * selectionMultiplier;
                final int outputSize = 9;
                hiddenSizes.addFirst(inputSize);
                hiddenSizes.add(outputSize);
                final int[] layerSizes = hiddenSizes.stream().mapToInt(v -> v).toArray();
                final ActivationFunction hiddenActivations = Utils.nameToActivation.get(act);
                final ActivationFunction outputActivation = new IdentityFunction(); // SHOULD NOT BE CHANGED
                final LossFunction lossFunction = Utils.nameToLoss.get(loss);

                net = new FFN(layerSizes, hiddenActivations, outputActivation, lossFunction);

                afterNetExists();
            });

            btnImport.setOnAction(_ -> {
                String path = mainWindow.selectFile(false);
                if (path == null)
                    return;
                net = FFN.load(path);

                afterNetExists();
            });

            btnExport.setOnAction(_ -> {
                String path = mainWindow.selectFile(true);
                if (path == null)
                    return;
                net.save(path);
            });


            btnDelete.setDisable(true);
            btnExport.setDisable(true);
            btnDelete.setOnAction(_ -> {
                net = null;

                tfHidden.setDisable(false);
                tfSelectionMultiplier.setDisable(false);
                cbAct.setDisable(false);
                cbLoss.setDisable(false);
                btnCreate.setDisable(false);
                btnImport.setDisable(false);
                btnExport.setDisable(true);
                btnDelete.setDisable(true);
            });

            HBox buttonRow = new HBox();
            buttonRow.setAlignment(Pos.CENTER_RIGHT);
            buttonRow.getChildren().addAll(btnCreate, btnImport, btnExport, btnDelete);

            Label lblAlpha = new Label("Learning rate (alpha):");
            tfAlpha = new TextField();
            cbAlphaDecay = new CheckBox("Decay Alpha over time");

            Label lblGamma = new Label("Discount factor (gamma):");
            tfGamma = new TextField();

            Label lblEpsilon = new Label("Exploration rate (epsilon):");
            tfEpsilon = new TextField();
            cbEpsilonDecay = new CheckBox("Decay Epsilon over time");

            Label lblTrainer = new Label("Trainer:");
            cbTrainer = new ComboBox<>();
            cbTrainer.getItems().addAll("Stochastic Gradient Descent", "Mini Batch", "Batch");
            cbTrainer.getSelectionModel().selectFirst();

            Label lblBatchSize = new Label("Batch size (int):");
            tfBatchSize = new TextField();
            lblBatchSize.setVisible(false);
            tfBatchSize.setVisible(false);

            cbTrainer.valueProperty().addListener((_, _, newVal) -> {
                lblBatchSize.setVisible(newVal.equals("Mini Batch"));
                tfBatchSize.setVisible(newVal.equals("Mini Batch"));
            });

            // Build layout
            box.getChildren().addAll(
                    lblTitle,
                    lblHidden, tfHidden,
                    lblSelectionMultiplier, tfSelectionMultiplier,
                    lblAct, cbAct,
                    lblLoss, cbLoss,
                    lblError,
                    lblSummary,
                    buttonRow,
                    lblAlpha, tfAlpha, cbAlphaDecay,
                    lblGamma, tfGamma,
                    lblEpsilon, tfEpsilon, cbEpsilonDecay,
                    lblTrainer, cbTrainer,
                    lblBatchSize, tfBatchSize
            );

            VBox.setVgrow(tfHidden, Priority.NEVER);

            // load prefs
            tfHidden.setText(prefs.get("hidden_sizes", "255"));
            tfSelectionMultiplier.setText(prefs.get("selection_multiplier", "3"));
            cbAct.setValue(prefs.get("activation", Utils.activationToName.get(new SigmoidFunction())));
            cbLoss.setValue(prefs.get("loss", Utils.lossToName.get(new MeanSquaredError())));
            tfAlpha.setText(prefs.get("alpha", "0.09"));
            cbAlphaDecay.setSelected(prefs.get("alpha_decay", "false").equals("true"));
            tfGamma.setText(prefs.get("gamma", "0.9"));
            tfEpsilon.setText(prefs.get("epsilon", "0.2"));
            cbEpsilonDecay.setSelected(prefs.get("epsilon_decay", "false").equals("true"));
            cbTrainer.setValue(prefs.get("trainer", cbTrainer.getItems().getFirst()));
            tfBatchSize.setText(prefs.get("batch_size", "32"));
        }

        private void afterNetExists() {
            tfHidden.setText(Arrays.stream(net.layerSizes)
                    .limit(net.layerSizes.length - 1)
                    .skip(1)
                    .mapToObj(Integer::toString)
                    .reduce((v1, v2) -> v1 + ", " + v2)
                    .orElseThrow());

            tfSelectionMultiplier.setText(Integer.toString((net.layerSizes[0] - 18 * 9) / 9));

            cbAct.setValue(Utils.activationToName.get(net.hiddenActivation));
            cbLoss.setValue(Utils.lossToName.get(net.lossFunction));

            prefs.put("hidden_sizes", tfHidden.getText().trim());
            prefs.put("selection_multiplier", tfSelectionMultiplier.getText().trim());
            prefs.put("activation", cbAct.getValue());
            prefs.put("loss", cbLoss.getValue());

            tfHidden.setDisable(true);
            tfSelectionMultiplier.setDisable(true);
            cbAct.setDisable(true);
            cbLoss.setDisable(true);
            btnCreate.setDisable(true);
            btnImport.setDisable(true);
            btnExport.setDisable(false);
            btnDelete.setDisable(false);
        }

        public VBox getPane() {
            return box;
        }

        public FFN getNet() {
            return net;
        }

        public NNParameters getNNParameters(FFN net) {
            double alpha, gamma, epsilon;
            FFNTrainer trainer;
            boolean alphaDecay, epsilonDecay;
            try {
                alpha = Double.parseDouble(tfAlpha.getText().trim().replace(",", "."));
                alphaDecay = cbAlphaDecay.isSelected();
                gamma = Double.parseDouble(tfGamma.getText().trim().replace(",", "."));
                epsilon = Double.parseDouble(tfEpsilon.getText().trim().replace(",", "."));
                epsilonDecay = cbEpsilonDecay.isSelected();
                switch (cbTrainer.getValue()) {
                    case "Stochastic Gradient Descent" -> trainer = new FFNTrainerSGD();
                    case "Mini Batch" -> {
                        int batchSize = Integer.parseInt(tfBatchSize.getText().trim());
                        prefs.put("batch_size", Integer.toString(batchSize));
                        trainer = new FFNTrainerMBGD(net.layerSizes, batchSize);
                    }
                    case "Batch" -> trainer = new FFNTrainerBGD(net.layerSizes);
                    default -> throw new InvalidParametersException("Invalid trainer selected.");
                }
            } catch (NumberFormatException ex) {
                throw new InvalidParametersException("Invalid NN parameters input for NN " + title + ". Alpha, gamma, epsilon, trainer & batchSize must be valid numbers.");
            }

            prefs.put("alpha", Double.toString(alpha));
            prefs.put("alpha_decay", Boolean.toString(alphaDecay));
            prefs.put("gamma", Double.toString(gamma));
            prefs.put("epsilon", Double.toString(epsilon));
            prefs.put("epsilon_decay", Boolean.toString(epsilonDecay));
            prefs.put("trainer", cbTrainer.getValue());


            return new NNParameters(alpha, gamma, epsilon, alphaDecay, epsilonDecay, trainer);
        }
    }

    private final MainWindow mainWindow;
    private final NNPanel[] nnPanels;

    public NNTab(MainWindow mainWindow, Preferences prefsPanel1, Preferences prefsPanel2) {
        this.mainWindow = mainWindow;

        setText("Neural Networks");
        setClosable(false);

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.HORIZONTAL);

        // Create left and right form
        nnPanels = new NNPanel[]{new NNPanel("NN 1", prefsPanel1)
                , new NNPanel("NN 2", prefsPanel2)
        };
        VBox left = nnPanels[0].getPane();
        VBox right = nnPanels[1].getPane();

        split.getItems().addAll(left, right);

        BorderPane root = new BorderPane();
        root.setCenter(split);
        // Wrap the root pane into a ScrollPane so the tab becomes scrollable
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setContent(scrollPane);
    }

    // Helper function: parses "64,32" -> List<Integer>
    private List<Integer> parseHiddenSizes(String text) throws NumberFormatException {
        List<Integer> out = new ArrayList<>();
        if (text == null) return out;
        String[] parts = text.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            int v = Integer.parseInt(t);
            if (v <= 0) throw new NumberFormatException("Value must be positive");
            out.add(v);
        }
        return out;
    }

    public FFN getNet(int idx) {
        if (idx < 0 || idx >= nnPanels.length) {
            throw new IllegalArgumentException("Invalid index: " + idx);
        }
        return nnPanels[idx].getNet();
    }

    public record NNParameters(double alpha, double gamma, double epsilon, boolean alphaDecay, boolean epsilonDecay,
                               FFNTrainer trainer) {
    }

    public NNParameters getNNParameters(int idx) {
        if (idx < 0 || idx >= nnPanels.length) {
            throw new IllegalArgumentException("Invalid index: " + idx);
        }
        return nnPanels[idx].getNNParameters(getNet(idx));
    }
}
