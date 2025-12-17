import gui.MainWindow;

void main() {
    // ATTENTION: the program would also START WITHOUT the javafx sdk included but this would lead to undefined behavior and bugs in the gui.
    // therefore this check assures that the javafx sdk was imported properly.
    if (ModuleLayer.boot().modules().stream().map(Module::getName).noneMatch(s -> s.contains("javafx")))
        throw new RuntimeException("javafx sdk not loaded properly. See README file.");

    MainWindow.run();
}
