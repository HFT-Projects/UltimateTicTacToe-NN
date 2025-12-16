package uttt.board;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Array;

public abstract class Board<T> {
    final protected T[][] board;

    @SuppressWarnings("unchecked")
    public Board(@NonNull Class<T> cls) {
        board = (T[][]) Array.newInstance(cls, 3, 3);
    }

    public T getCell(Selection sel) {
        return board[sel.idxRow()][sel.idxColumn()];
    }

    public abstract ENDED_STATUS calculateEndedStatus();
}
