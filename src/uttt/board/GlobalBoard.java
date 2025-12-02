package uttt.board;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import uttt.actor.PLAYER;
import helper.Utils;

public class GlobalBoard extends Board<LocalBoard> {
    public GlobalBoard() {
        super(LocalBoard.class);
        for (int i = 0; i < 3; i++)
            for (int k = 0; k < 3; k++)
                board[i][k] = new LocalBoard(new Selection(i, k));
    }

    public PLAYER[][] getState() {
        PLAYER[][] state = new PLAYER[9][];
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                PLAYER[] localState = getCell(new Selection(i, k)).getState();
                state[3*i+k] = localState;
            }
        }
        return state;
    }

    @Override
   boolean won(Selection cell1, Selection cell2, Selection cell3, @NonNull AtomicReference<PLAYER> wonRef) {
        ENDED_STATUS b1e = getCell(cell1).ended();
        ENDED_STATUS b2e = getCell(cell2).ended();
        ENDED_STATUS b3e = getCell(cell3).ended();

        PLAYER b1w = Utils.ENDED_STATUS_TO_PLAYER.get(b1e);
        PLAYER b2w = Utils.ENDED_STATUS_TO_PLAYER.get(b2e);
        PLAYER b3w = Utils.ENDED_STATUS_TO_PLAYER.get(b3e);

        if (b1w != null && b1w == b2w && b1w == b3w) {
            wonRef.set(b1w);
            return true;
        }
        return false;
    }

    @Override
    boolean tied() {
        // check if all local boards are ended
        boolean tied = true;
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                LocalBoard board = getCell(new Selection(i, k));
                if (board.ended() == null) {
                    tied = false;
                    break;
                }
            }
        }
        return tied;
    }

    public Selection[] getPlayableLocalBoards() {
        List<LocalBoard> boards = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                LocalBoard board = getCell(new Selection(i, k));
                if (board.ended() == null)
                    boards.add(board);
            }
        }
        return boards.stream().map(LocalBoard::getSelection).toArray(Selection[]::new);
    }
}
