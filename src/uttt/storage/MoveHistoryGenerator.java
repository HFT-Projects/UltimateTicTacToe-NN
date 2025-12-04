package uttt.storage;

import uttt.observer.Event;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class MoveHistoryGenerator {
    private final List<Move> history = new LinkedList<>();

    public Move[] getHistory() {
        return history.toArray(new Move[0]);
    }

    public void handleEvent(Event event) {
        history.add(new Move(event.player(), event.board(), event.action(), event.localEndedStatus(), event.globalEndedStatus()));
    }
}