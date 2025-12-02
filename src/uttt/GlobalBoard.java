package uttt;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GlobalBoard extends Board<LocalBoard> {
    public GlobalBoard() {
        super(LocalBoard.class);
        for (int i = 0; i < 3; i++)
            for (int k = 0; k < 3; k++)
                board[i][k] = new LocalBoard(i, k);
    }

    public LocalBoard[] getRemainingLocalBoards() {
        List<LocalBoard> boards = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            LocalBoard board = getCell(i);
            if (board.ended() == null)
                boards.add(board);
        }
        return boards.toArray(new LocalBoard[0]);
    }

    public boolean[] getState() {
        // 9 local boards, each with 18 booleans
        boolean[] state = new boolean[9 * 18];
        for (int i = 0; i < 9; i++) {
            boolean[] localState = getCell(i).getState();
            System.arraycopy(localState, 0, state, i * 18, 18);
        }
        return state;
    }

    public double[] getStateDouble() {
        // convert boolean[] to double[]
        boolean[] state = getState();
        Stream<Boolean> stateStream = IntStream.range(0, state.length)
                .mapToObj(idx -> state[idx]);
        return stateStream.mapToDouble(b -> b ? 1d : 0d).toArray();
    }

    @Override
    public boolean won(int idx1r, int idx1c, int idx2r, int idx2c, int idx3r, int idx3c, @NonNull AtomicReference<PLAYER> wonRef) {
        ENDED_STATUS b1e = getCell(idx1r, idx1c).ended();
        ENDED_STATUS b2e = getCell(idx2r, idx2c).ended();
        ENDED_STATUS b3e = getCell(idx3r, idx3c).ended();

        PLAYER b1w = ENDED_STATUS_TO_PLAYER.get(b1e);
        PLAYER b2w = ENDED_STATUS_TO_PLAYER.get(b2e);
        PLAYER b3w = ENDED_STATUS_TO_PLAYER.get(b3e);

        if (b1w != null && b1w == b2w && b1w == b3w) {
            wonRef.set(b1w);
            return true;
        }
        return false;
    }

    @Override
    public boolean tied() {
        // todo: better tie detection
        // check if all local boards are ended
        boolean tied = true;
        for (int i = 0; i < 9; i++) {
            LocalBoard board = getCell(i);
            if (board.ended() == null) {
                tied = false;
                break;
            }
        }
        return tied;
    }
}
