package uttt;

import helper.Utils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import helper.Stats;
import uttt.actor.Actor;
import uttt.actor.PLAYER;
import uttt.board.Selection;
import uttt.board.ENDED_STATUS;
import uttt.board.GlobalBoard;
import uttt.board.LocalBoard;
import uttt.observer.Observer;
import uttt.observer.Event;

public class Game {
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean DEBUG_PRINT_EACH_MOVE = false;

    private final GlobalBoard globalBoard = new GlobalBoard();
    private final Actor actorX;
    private final Actor actorO;
    private final List<Observer> observers = new ArrayList<>();

    public Game(Actor actorX, Actor actorO) {
        this(actorX, actorO, new Observer[0]);
    }

    public Game(Actor actorX, Actor actorO, Observer[] observers) {
        this.actorX = actorX;
        this.actorO = actorO;
        this.observers.addAll(Arrays.asList(observers));
    }

    public ENDED_STATUS run() {
        Selection actionX;
        Selection actionO = null;

        while (true) {
            actionX = move(globalBoard, actorX, actionO);

            // exit if game has ended
            if (globalBoard.ended() != null) {
                break;
            }

            actionO = move(globalBoard, actorO, actionX);

            // exit if game has ended
            if (globalBoard.ended() != null) {
                break;
            }
        }

        System.out.println(Stats.boardToString(globalBoard));

        return globalBoard.ended();
    }

    private Selection move(GlobalBoard globalBoard, Actor actor, @Nullable Selection desiredBoardIdx) {
        PLAYER[][] state = globalBoard.getState();

        LocalBoard localBoard = null;
        if (desiredBoardIdx != null)
            localBoard = globalBoard.getCell(desiredBoardIdx);

        // choose new board if the predetermined one is ended or not set
        if (desiredBoardIdx == null || localBoard.ended() != null)
            localBoard = globalBoard.getCell(actor.chooseBoard(state, globalBoard.getPlayableLocalBoards()));

        Selection action = actor.move(state, localBoard.getSelection(), localBoard.getPlayableActions());
        localBoard.setCell(action, actor.getPlayer());

        ENDED_STATUS endedStatus = globalBoard.ended();
        notifyObservers(actor, localBoard.getSelection(), action, state, globalBoard.getState(), endedStatus, localBoard.ended());

        // LOGGING
        if (DEBUG_PRINT_EACH_MOVE) {
            String out = Stats.boardToString(globalBoard, Utils.selectionToInt(localBoard.getSelection()), Utils.selectionToInt(action));
            System.out.println(out);
        }

        return action;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @SuppressWarnings("unused")
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Actor actor, Selection boardSel, Selection actionSel, PLAYER[][] oldState, PLAYER[][] newState, ENDED_STATUS globalEndedStatus, ENDED_STATUS localEndedStatus) {
        Event e = new Event(actor.getPlayer(), boardSel, actionSel, oldState, newState, globalEndedStatus, localEndedStatus);
        for (Observer observer : observers) {
            observer.notify(e);
        }
    }
}
