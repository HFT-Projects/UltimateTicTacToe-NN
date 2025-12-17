package uttt.board;

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
        board = new LocalBoard(0);
    }

    @AfterEach
    public void teardown() {
        board = null;
    }

    @Test
    public void getStateTest() {
        board.setCell(0, PLAYER.X);
        board.setCell(5, PLAYER.X);
        board.setCell(8, PLAYER.O);

        PLAYER[] expectedState = {PLAYER.X, null, null, null, null, PLAYER.X, null, null, PLAYER.O};
        assertArrayEquals(board.getState(), expectedState);
    }

    @Test
    public void ThrowCellAlreadySetException() {
        board.setCell(0, PLAYER.X);
        assertThrows(CellAlreadySetException.class, () -> board.setCell(0, PLAYER.O));
    }

    @Test
    public void getValidActionsTest() {
        int[] validActions = board.getPlayableActions();
        int[] expectedValidActions = IntStream.iterate(0, i -> i + 1).limit(9).toArray();
        assertArrayEquals(validActions, expectedValidActions);

        board.setCell(8, PLAYER.X);
        board.setCell(7, PLAYER.O);
        board.setCell(6, PLAYER.O);

        validActions = board.getPlayableActions();
        expectedValidActions = IntStream.iterate(0, i -> i + 1).limit(6).toArray();
        assertArrayEquals(validActions, expectedValidActions);
    }

    @Test
    public void testWinRowsCalculateEndedStatus() {
        for (int r = 0; r < 3; r++) {
            board = new LocalBoard(0);
            board.setCell(3 * r, PLAYER.X);
            board.setCell(3 * r + 1, PLAYER.X);
            board.setCell(3 * r + 2, PLAYER.X);
            assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());
        }
    }

    @Test
    public void testWinColumnsCalculateEndedStatus() {
        for (int c = 0; c < 3; c++) {
            board = new LocalBoard(0);
            board.setCell(c, PLAYER.X);
            board.setCell(3 + c, PLAYER.X);
            board.setCell(6 + c, PLAYER.X);
            assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());
        }
    }

    @Test
    public void testWinDiagonalsCalculateEndedStatus() {
        board = new LocalBoard(0);
        board.setCell(0, PLAYER.X);
        board.setCell(4, PLAYER.X);
        board.setCell(8, PLAYER.X);
        assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());

        board = new LocalBoard(0);
        board.setCell(2, PLAYER.X);
        board.setCell(4, PLAYER.X);
        board.setCell(6, PLAYER.X);
        assertEquals(ENDED_STATUS.X, board.calculateEndedStatus());
    }

    @Test
    public void testCalculateEndedStatusNotEnded() {
        board = new LocalBoard(0);
        board.setCell(0, PLAYER.X);
        board.setCell(3, PLAYER.X);
        board.setCell(2, PLAYER.X);
        assertNull(board.calculateEndedStatus());
    }

    @Test
    public void testTiedCalculateEndedStatus() {
        board = new LocalBoard(0);
        // fill board without any winning line -> tie expected
        // O X O
        // X X O
        // O O X
        int[] xIndices = {1, 3, 4, 8};
        for (int i = 0; i < 9; i++) {
            boolean isX = false;
            for (int x : xIndices)
                if (i == x) {
                    isX = true;
                    break;
                }
            board.setCell(i, isX ? PLAYER.X : PLAYER.O);
        }
        assertEquals(ENDED_STATUS.TIE, board.calculateEndedStatus());
    }

    @Test
    public void testWinOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> board.setCell(12, PLAYER.X));
    }
}
