package uttt.board;

import java.util.ArrayList;
import java.util.List;

import uttt.actor.PLAYER;
import helper.Utils;

public class GlobalBoard extends Board<LocalBoard> {
    public GlobalBoard() {
        super(LocalBoard.class);
        for (int i = 0; i < 9; i++)
            board[i] = new LocalBoard(i);
    }

    public PLAYER[][] getState() {
        PLAYER[][] state = new PLAYER[9][];
        for (int i = 0; i < 9; i++) {
            PLAYER[] localState = getCell(i).getState();
            state[i] = localState;
        }

        return state;
    }

    @Override
    public ENDED_STATUS calculateEndedStatus() {
        return Utils.globalEnded(getState());
    }

    public int[] getPlayableLocalBoards() {
        List<LocalBoard> boards = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            LocalBoard board = getCell(i);
            if (board.calculateEndedStatus() == null)
                boards.add(board);
        }
        return boards.stream().map(LocalBoard::getIdx).mapToInt(Integer::valueOf).toArray();
    }
}
