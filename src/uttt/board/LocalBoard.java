package uttt.board;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import uttt.actor.PLAYER;

public class LocalBoard extends Board<PLAYER> {
    private final Selection selection;

    public LocalBoard(Selection sel) {
        selection = sel;
        super(PLAYER.class);
    }

    public Selection getSelection() {
        return selection;
    }

    public void setCell(Selection sel, PLAYER player) {
        if (getCell(sel) != null)
            throw new CellAlreadySetException();
        board[sel.idxRow()][sel.idxColumn()] = player;
    }

    public PLAYER[] getState() {
        return Arrays.stream(board).flatMap(Arrays::stream).toArray(PLAYER[]::new);
    }

    public Selection[] getPlayableActions() {
        // get all valid actions -> empty cells
        List<Selection> actions = new ArrayList<>();

        for (int i = 0; i < 3; i++)
            for (int k = 0; k < 3; k++)
                if (getCell(new Selection(i, k)) == null)
                    actions.add(new Selection(i, k));

        return actions.toArray(new Selection[0]);
    }

    @Override
    boolean won(Selection cell1, Selection cell2, Selection cell3, @NonNull AtomicReference<PLAYER> wonRef) {
        PLAYER b1w = getCell(cell1);
        PLAYER b2w = getCell(cell2);
        PLAYER b3w = getCell(cell3);

        if (b1w != null && b1w == b2w && b1w == b3w) {
            wonRef.set(b1w);
            return true;
        }
        return false;
    }

    @Override
    boolean tied() {
        // check if all cells are filled
        boolean tied = true;
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                if (getCell(new Selection(i, k)) == null) {
                    tied = false;
                    break;
                }
            }
        }
        return tied;
    }
}
