package uttt.observer;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

public record Event(PLAYER player, int board, int action, PLAYER[][] oldState,
                    PLAYER[][] newState, ENDED_STATUS globalEndedStatus, ENDED_STATUS localEndedStatus) {
}
