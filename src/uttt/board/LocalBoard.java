package uttt.board;

import org.jspecify.annotations.NonNull;
import uttt.board.Board.PLAYER;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LocalBoard extends Board<PLAYER> {
    public static class CellAlreadySetException extends RuntimeException {
    }

    public final int idxR;
    public final int idxC;

    public LocalBoard(int idxR, int idxC) {
        this.idxR = idxR;
        this.idxC = idxC;
        super(PLAYER.class);
    }

    public int getIdx() {
        return idxR * 3 + idxC;
    }

    public void setCell(int idxr, int idxc, PLAYER player) {
        if (getCell(idxr, idxc) != null)
            throw new CellAlreadySetException();
        board[idxr][idxc] = player;
    }

    public void setCell(int idx, PLAYER player) {
        setCell(idx / 3, idx % 3, player);
    }

    @Override
    public boolean won(int idx1r, int idx1c, int idx2r, int idx2c, int idx3r, int idx3c, @NonNull AtomicReference<PLAYER> wonRef) {
        PLAYER b1w = getCell(idx1r, idx1c);
        PLAYER b2w = getCell(idx2r, idx2c);
        PLAYER b3w = getCell(idx3r, idx3c);

        if (b1w != null && b1w == b2w && b1w == b3w) {
            wonRef.set(b1w);
            return true;
        }
        return false;
    }

    public CELL_STATE[] getState(PLAYER player) {
        // get board state encoded as boolean[] for NN input
        // 2 bits per cell: 00 = empty, 01 = O, 10 = X
        CELL_STATE[] ret = new CELL_STATE[9];
        for (int i = 0; i < 9; i++) {
            PLAYER cell = getCell(i);

            if (cell == player) {
                ret[i] = CELL_STATE.YOU;
            } else if (cell != null) {
                ret[i] = CELL_STATE.ENEMY;
            } else {
                ret[i] = CELL_STATE.NOT_SET;
            }
        }
        return ret;
    }

    public int[] getValidActions() {
        // get all valid actions -> empty cells
        List<Integer> actions = new ArrayList<>();

        for (int i = 0; i < 3; i++)
            for (int k = 0; k < 3; k++)
                if (getCell(i, k) == null)
                    actions.add(i * 3 + k);

        return actions.stream().mapToInt(i -> i).toArray();
    }

    @Override
    public boolean tied() {
        // todo: better tie detection
        // check if all cells are filled
        boolean tied = true;
        for (int i = 0; i < 9; i++) {
            if (getCell(i) == null) {
                tied = false;
                break;
            }
        }
        return tied;
    }
}
