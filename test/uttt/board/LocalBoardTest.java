package uttt.board;

import helper.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import uttt.actor.PLAYER;

import java.util.stream.IntStream;

public class LocalBoardTest {
    private LocalBoard board;

    @BeforeEach
    public void setup() {
        board = new LocalBoard(new Selection(0, 0));
    }

    @AfterEach
    public void teardown() {
        board = null;
    }

    @Test
    public void getStateTest() {
        board.setCell(new Selection(0, 0), PLAYER.X);
        board.setCell(new Selection(1, 2), PLAYER.X);
        board.setCell(new Selection(2, 2), PLAYER.O);

        PLAYER[] expectedState = {PLAYER.X, null, null, null, null, PLAYER.X, null, null, PLAYER.O};
        assertArrayEquals(board.getState(), expectedState);
    }

    @Test
    public void ThrowCellAlreadySetException() {
        board.setCell(new Selection(0, 0), PLAYER.X);
        assertThrows(CellAlreadySetException.class, () -> board.setCell(new Selection(0, 0), PLAYER.O));
    }

    @Test
    public void getValidActionsTest() {
        Selection[] validActions = board.getPlayableActions();
        Selection[] expectedValidActions = IntStream.iterate(0, i -> i + 1).limit(9).mapToObj(Utils::intToSelection).toArray(Selection[]::new);
        assertArrayEquals(validActions, expectedValidActions);

        board.setCell(new Selection(2, 2), PLAYER.X);
        board.setCell(new Selection(2, 1), PLAYER.O);
        board.setCell(new Selection(2, 0), PLAYER.O);

        validActions = board.getPlayableActions();
        expectedValidActions = IntStream.iterate(0, i -> i + 1).limit(6).mapToObj(Utils::intToSelection).toArray(Selection[]::new);
        assertArrayEquals(validActions, expectedValidActions);
    }

    @Test
    public void testWinRowsCalculateEndedStatus() {
        for (int r = 0; r < 3; r++) {
            board = new LocalBoard(new Selection(0, 0));
            board.setCell(new Selection(r, 0), PLAYER.X);
            board.setCell(new Selection(r, 1), PLAYER.X);
            board.setCell(new Selection(r, 2), PLAYER.X);
            assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());
        }
    }

    @Test
    public void testWinColumnsCalculateEndedStatus() {
        for (int c = 0; c < 3; c++) {
            board = new LocalBoard(new Selection(0, 0));
            board.setCell(new Selection(0, c), PLAYER.X);
            board.setCell(new Selection(1, c), PLAYER.X);
            board.setCell(new Selection(2, c), PLAYER.X);
            assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());
        }
    }

    @Test
    public void testWinDiagonalsCalculateEndedStatus() {
        board = new LocalBoard(new Selection(0, 0));
        board.setCell(new Selection(0, 0), PLAYER.X);
        board.setCell(new Selection(1, 1), PLAYER.X);
        board.setCell(new Selection(2, 2), PLAYER.X);
        assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());

        board = new LocalBoard(new Selection(0, 0));
        board.setCell(new Selection(0, 2), PLAYER.X);
        board.setCell(new Selection(1, 1), PLAYER.X);
        board.setCell(new Selection(2, 0), PLAYER.X);
        assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());
    }

    @Test
    public void testCalculateEndedStatusNotEnded() {
        board = new LocalBoard(new Selection(0, 0));
        board.setCell(new Selection(0, 0), PLAYER.X);
        board.setCell(new Selection(1, 0), PLAYER.X);
        board.setCell(new Selection(0, 2), PLAYER.X);
        assertNull(board.calculateEndedStatus());
    }

    @Test
    public void testTiedCalculateEndedStatus() {
        board = new LocalBoard(new Selection(0, 0));
        // fill board without any winning line -> tie expected
        // O X O
        // X X O
        // O O X
        int[] xIndices = {1, 3, 4, 8};
        for (int i = 0; i < 9; i++) {
            Selection sel = Utils.intToSelection(i);
            boolean isX = false;
            for (int x : xIndices)
                if (i == x) {
                    isX = true;
                    break;
                }
            board.setCell(sel, isX ? PLAYER.X : PLAYER.O);
        }
        assertEquals(ENDED_STATUS.TIE, board.calculateEndedStatus());
    }

    @Test
    public void testWinOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> board.setCell(new Selection(3, 3), PLAYER.X));
    }
}
