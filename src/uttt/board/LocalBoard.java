package uttt.board;

import helper.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Override
    public ENDED_STATUS calculateEndedStatus() {
        return Utils.localEnded(getState());
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
}
