package uttt.board;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Array;

public abstract class Board<T> {
    final protected T[] board;

    public Board(@NonNull Class<T> cls) {
        //noinspection unchecked
        board = (T[]) Array.newInstance(cls, 9);
    }

    public T getCell(int idx) {
        return board[idx];
    }

    @SuppressWarnings("unused")
    public abstract ENDED_STATUS calculateEndedStatus();
}
