package uttt.board;

import helper.Utils;

import java.util.ArrayList;
import java.util.List;

import uttt.actor.PLAYER;

public class LocalBoard extends Board<PLAYER> {
    private final int idx;

    public LocalBoard(int idx) {
        this.idx = idx;
        super(PLAYER.class);
    }

    public int getIdx() {
        return idx;
    }

    public void setCell(int idx, PLAYER player) {
        if (getCell(idx) != null)
            throw new CellAlreadySetException();
        board[idx] = player;
    }

    public PLAYER[] getState() {
        return board.clone();
    }

    @Override
    public ENDED_STATUS calculateEndedStatus() {
        return Utils.localEnded(getState());
    }

    public int[] getPlayableActions() {
        // get all valid actions -> empty cells
        List<Integer> actions = new ArrayList<>();

        for (int i = 0; i < 9; i++)
            if (getCell(i) == null)
                actions.add(i);

        return actions.stream().mapToInt(Integer::valueOf).toArray();
    }
}
