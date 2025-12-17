package gui.utils;

import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GUIUtils {
    private GUIUtils() {
    }

    public static void runPlatformLaterBlocking(Runnable runnable) {
        AtomicBoolean isDone = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                runnable.run();
            } finally {
                isDone.set(true);
            }
        });

        while (!isDone.get()) {
            try {
                //noinspection BusyWait
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
