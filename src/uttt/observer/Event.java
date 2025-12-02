package uttt.observer;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.Selection;

public record Event(PLAYER player, Selection board, Selection action, PLAYER[][] oldState,
                    PLAYER[][] newState, ENDED_STATUS globalEndedStatus, ENDED_STATUS localEndedStatus) {
}
