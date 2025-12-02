package helper;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.Selection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Utils {
    private Utils() {
    }

    public static final Map<ENDED_STATUS, PLAYER> ENDED_STATUS_TO_PLAYER;
    public static final Map<PLAYER, ENDED_STATUS> PLAYER_TO_ENDED_STATUS;

    static {
        // create unmodifiable maps for conversions

        Map<ENDED_STATUS, PLAYER> endedStatusToPlayer = new HashMap<>();
        endedStatusToPlayer.put(ENDED_STATUS.X, PLAYER.X);
        endedStatusToPlayer.put(ENDED_STATUS.O, PLAYER.O);
        endedStatusToPlayer.put(ENDED_STATUS.TIE, null);
        endedStatusToPlayer.put(null, null);
        ENDED_STATUS_TO_PLAYER = Collections.unmodifiableMap(endedStatusToPlayer);

        Map<PLAYER, ENDED_STATUS> playerToEndedStatus = new HashMap<>();
        playerToEndedStatus.put(PLAYER.X, ENDED_STATUS.X);
        playerToEndedStatus.put(PLAYER.O, ENDED_STATUS.O);
        playerToEndedStatus.put(null, null);
        PLAYER_TO_ENDED_STATUS = Collections.unmodifiableMap(playerToEndedStatus);
    }

    public static int selectionToInt(Selection sel) {
        return sel.idxRow() * 3 + sel.idxColumn();
    }
}
