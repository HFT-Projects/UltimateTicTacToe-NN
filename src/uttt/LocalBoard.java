package uttt;

import org.jspecify.annotations.NonNull;
import uttt.Board.PLAYER;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LocalBoard extends Board<PLAYER> {
    static class CellAlreadySetException extends RuntimeException {
    }

    protected LocalBoard() {
        super(PLAYER.class);
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
    protected boolean won(int idx1r, int idx1c, int idx2r, int idx2c, int idx3r, int idx3c, @NonNull AtomicReference<PLAYER> wonRef) {
        PLAYER b1w = getCell(idx1r, idx1c);
        PLAYER b2w = getCell(idx2r, idx2c);
        PLAYER b3w = getCell(idx3r, idx3c);

        if (b1w != null && b1w == b2w && b1w == b3w) {
            wonRef.set(b1w);
            return true;
        }
        return false;
    }

    public boolean[] getState() {
        // get board state encoded as boolean[] for NN input
        // 2 bits per cell: 00 = empty, 01 = O, 10 = X
        boolean[] ret = new boolean[18];
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                PLAYER cell = getCell(i, k);

                // linear_index is from 0 to 8 (in 3x3 board)
                int linear_index = 3 * i + k;
                // index in boolean[] for cell (i,k)
                // each cell uses 2 bits
                int idx1 = 2 * linear_index;
                int idx2 = idx1 + 1;

                if (cell == PLAYER.X) {
                    ret[idx1] = true;
                    ret[idx2] = false;
                } else if (cell == PLAYER.O) {
                    ret[idx1] = false;
                    ret[idx2] = true;
                } else {
                    ret[idx1] = false;
                    ret[idx2] = false;
                }
            }
        }
        return ret;
    }

    public double[] getStateDouble() {
        // convert boolean[] to double[]
        boolean[] state = getState();
        Stream<Boolean> stateStream = IntStream.range(0, state.length)
                .mapToObj(idx -> state[idx]);
        return stateStream.mapToDouble(b -> b ? 1d : 0d).toArray();
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
    protected boolean tied() {
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
