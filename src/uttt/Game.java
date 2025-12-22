package uttt;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uttt.actor.Actor;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.GlobalBoard;
import uttt.board.LocalBoard;
import uttt.observer.Observer;
import uttt.observer.Event;

public class Game {
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

        if (actorX.getPlayer() != PLAYER.X || actorO.getPlayer() != PLAYER.O) {
            throw new IllegalArgumentException("Actors must be assigned to correct players.");
        }

        this.observers.addAll(Arrays.asList(observers));
    }

    public ENDED_STATUS run() {
        int actionX;
        Integer actionO = null;

        while (true) {
            actionX = move(globalBoard, actorX, actionO);

            // exit if game has ended
            if (globalBoard.calculateEndedStatus() != null) {
                break;
            }

            actionO = move(globalBoard, actorO, actionX);

            // exit if game has ended
            if (globalBoard.calculateEndedStatus() != null) {
                break;
            }
        }

        return globalBoard.calculateEndedStatus();
    }

    private int move(GlobalBoard globalBoard, Actor actor, @Nullable Integer desiredBoardIdx) {
        PLAYER[][] state = globalBoard.getState();

        LocalBoard localBoard = null;
        if (desiredBoardIdx != null)
            localBoard = globalBoard.getCell(desiredBoardIdx);

        // choose new board if the predetermined one is ended or not set
        if (desiredBoardIdx == null || localBoard.calculateEndedStatus() != null)
            localBoard = globalBoard.getCell(actor.chooseBoard(state, globalBoard.getPlayableLocalBoards()));

        int action = actor.move(state, localBoard.getIdx(), localBoard.getPlayableActions());
        localBoard.setCell(action, actor.getPlayer());

        ENDED_STATUS endedStatus = globalBoard.calculateEndedStatus();
        notifyObservers(actor, localBoard.getIdx(), action, state, globalBoard.getState(), endedStatus, localBoard.calculateEndedStatus());

        return action;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @SuppressWarnings("unused")
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Actor actor, int boardSel, int actionSel, PLAYER[][] oldState, PLAYER[][] newState, ENDED_STATUS globalEndedStatus, ENDED_STATUS localEndedStatus) {
        Event e = new Event(actor.getPlayer(), boardSel, actionSel, oldState, newState, globalEndedStatus, localEndedStatus);
        for (Observer observer : observers) {
            observer.notify(e);
        }
    }
}
