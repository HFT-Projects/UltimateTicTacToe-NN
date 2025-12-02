package uttt.board;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Board<T> {
    public enum PLAYER {
        X,
        O
    }

    public enum ENDED_STATUS {
        X,
        O,
        TIE
    }

    public enum CELL_STATE {
        YOU,
        ENEMY,
        NOT_SET
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

    final protected T[][] board;

    @SuppressWarnings("unchecked")
    public Board(@NonNull Class<T> cls) {
        board = (T[][]) Array.newInstance(cls, 3, 3);
    }

    public T getCell(int idxr, int idxc) {
        return board[idxr][idxc];
    }

    public T getCell(int idx) {
        return board[idx / 3][idx % 3];
    }

    protected abstract boolean won(int idx1r, int idx1c, int idx2r, int idx2c, int idx3r, int idx3c, @NonNull AtomicReference<PLAYER> wonRef);

    protected abstract boolean tied();

    public ENDED_STATUS ended() {
        AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        // check all winning combinations
        if (won(0, 0, 0, 1, 0, 2, wonRef) ||
                won(1, 0, 1, 1, 1, 2, wonRef) ||
                won(2, 0, 2, 1, 2, 2, wonRef) ||

                won(0, 0, 1, 0, 2, 0, wonRef) ||
                won(0, 1, 1, 1, 2, 1, wonRef) ||
                won(0, 2, 1, 2, 2, 2, wonRef) ||

                won(0, 0, 1, 1, 2, 2, wonRef) ||
                won(0, 2, 1, 1, 2, 0, wonRef)
        ) {
            PLAYER won = wonRef.get();
            // sanity check
            if (won == null)
                throw new RuntimeException();
            return PLAYER_TO_ENDED_STATUS.get(won);
        }

        if (tied())
            return ENDED_STATUS.TIE;

        return null;
    }
}
