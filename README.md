# UltimateTicTacToe-NN

## Overview

The UltimateTicTacToe-NN is a software project developed as part of an academic assignment for the Artificial Intelligence course at [HFT Stuttgart](https://hft-stuttgart.com/). The project focuses on implementing a neural network-based system to play Ultimate Tic Tac Toe, providing both a graphical user interface (GUI) for human interaction and an autonomous runner for long-term training.

### Key Features

- **Graphical User Interface (GUI)**: Allows human players to interact with the game.
- **Autonomous Training**: Enables automated training of the neural network on a server.
- **Neural Network Implementation**: Includes various activation functions, loss functions, and training algorithms.
- **Game Logic**: Implements the rules and mechanics of Ultimate Tic Tac Toe.

## Setting up the Development Environment

1. use **Java 25 (V. 25.0.1)**

2. Download JavaFX sdk (V. 25.0.1) [here](https://gluonhq.com/products/javafx/)

3. Create run configuration
   1. Create a new run configuration

   2. Add the following to the VM-options: \
      `--module-path "%javafx-sdk%/lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics` \
      replace `%javafx-sdk%` with the path of the unarchived javafx sdk.

## Main Classes

- **GUI**: The primary main class for human interaction.
- **AutonomousRunner**: The main class for automated training.

## UML Diagram

![UML-Diagram](https://i.ibb.co/qMz92P2k/UML-Diagram-Ultimate-Tic-Tac-Toe-NN-2.png)

The GUI classes are omitted in the UML diagram for simplicity.
