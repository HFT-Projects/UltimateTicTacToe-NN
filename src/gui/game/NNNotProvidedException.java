package gui.game;

public class NNNotProvidedException extends Exception {
    public final int idx;

    public NNNotProvidedException(int idx, String message) {
        super(message);
        this.idx = idx;
    }
}
