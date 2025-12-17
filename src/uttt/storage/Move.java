package uttt.storage;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

public record Move(PLAYER player, int board, int action, ENDED_STATUS localEndedStatus,
                   ENDED_STATUS globalEndedStatus) {
}
