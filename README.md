# UltimateTicTacToe-NN

## Setting up the development environment

1. use **Java 25 (V. 25.0.1)**

2. Download JavaFX sdk (V. 25.0.1) [here](https://gluonhq.com/products/javafx/)

3. Create run configuration

    1. Create a new run configuration

    2. Add the following to the VM-options: \
       `--module-path "%javafx-sdk%/lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics` \
       replace `%javafx-sdk%` with the path of the unarchived javafx sdk.

## Main Classes

There are two main classes: GUI and AutonomousRunner. \
GUI is the main class intended for use by human and the primary main class. \
AutonomousRunner is the main class intended to use for automated long-term training of the neural network on a server.