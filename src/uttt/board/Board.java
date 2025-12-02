package uttt.board;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicReference;

import uttt.actor.PLAYER;
import helper.Utils;

public abstract class Board<T> {
    final protected T[][] board;

    @SuppressWarnings("unchecked")
    public Board(@NonNull Class<T> cls) {
        board = (T[][]) Array.newInstance(cls, 3, 3);
    }

    public T getCell(Selection sel) {
        return board[sel.idxRow()][sel.idxColumn()];
    }

    abstract boolean won(Selection cell1, Selection cell2, Selection cell3, @NonNull AtomicReference<PLAYER> wonRef);

    abstract boolean tied();

    public ENDED_STATUS ended() {
        AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        // check all winning combinations
        if (won(new Selection(0, 0), new Selection(0, 1), new Selection(0, 2), wonRef) ||
                won(new Selection(1, 0), new Selection(1, 1), new Selection(1, 2), wonRef) ||
                won(new Selection(2, 0), new Selection(2, 1), new Selection(2, 2), wonRef) ||

                won(new Selection(0, 0), new Selection(1, 0), new Selection(2, 0), wonRef) ||
                won(new Selection(0, 1), new Selection(1, 1), new Selection(2, 1), wonRef) ||
                won(new Selection(0, 2), new Selection(1, 2), new Selection(2, 2), wonRef) ||

                won(new Selection(0, 0), new Selection(1, 1), new Selection(2, 2), wonRef) ||
                won(new Selection(0, 2), new Selection(1, 1), new Selection(2, 0), wonRef)
        ) {
            PLAYER won = wonRef.get();
            // sanity check
            if (won == null)
                throw new RuntimeException();
            return Utils.PLAYER_TO_ENDED_STATUS.get(won);
        }

        if (tied())
            return ENDED_STATUS.TIE;

        return null;
    }
}
