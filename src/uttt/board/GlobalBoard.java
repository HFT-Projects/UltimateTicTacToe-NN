package uttt.board;

import java.util.ArrayList;
import java.util.List;

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
    public ENDED_STATUS calculateEndedStatus() {
        return Utils.globalEnded(getState());
    }

    public Selection[] getPlayableLocalBoards() {
        List<LocalBoard> boards = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                LocalBoard board = getCell(new Selection(i, k));
                if (board.calculateEndedStatus() == null)
                    boards.add(board);
            }
        }
        return boards.stream().map(LocalBoard::getSelection).toArray(Selection[]::new);
    }
}
