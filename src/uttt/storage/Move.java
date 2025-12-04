package uttt.storage;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.Selection;

public record Move(PLAYER player, Selection board, Selection action, ENDED_STATUS localEndedStatus,
                   ENDED_STATUS globalEndedStatus) {
}
