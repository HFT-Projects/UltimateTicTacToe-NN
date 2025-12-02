package uttt.observer;

import java.util.function.Consumer;

@SuppressWarnings("ClassCanBeRecord")
public class Observer {
    private final Consumer<Event> callback;

    public Observer(Consumer<Event> callback) {
        this.callback = callback;
    }

    @SuppressWarnings("unused")
    public void notify(Event event) {
        callback.accept(event);
    }
}
