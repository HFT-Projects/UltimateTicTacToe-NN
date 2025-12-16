package uttt.board;

import helper.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import uttt.actor.PLAYER;

import java.util.concurrent.atomic.AtomicReference;
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
    public void testTied() {
        assertFalse(board.tied());
        for (int i = 0; i < 9; i++) {
            board.setCell(Utils.intToSelection(i), PLAYER.O);
        }
        assertTrue(board.tied());
    }

    @Test
    public void testWinAllRows() {
        for (int i = 0; i < 3; i++) {
            final AtomicReference<PLAYER> wonRef = new AtomicReference<>();

            board.setCell(new Selection(i, 0), PLAYER.X);
            board.setCell(new Selection(i, 1), PLAYER.X);
            board.setCell(new Selection(i, 2), PLAYER.X);

            assertTrue(board.won(new Selection(i, 0), new Selection(i, 1), new Selection(i, 2), wonRef));
            assertEquals(PLAYER.X, wonRef.get());
        }
    }

    @Test
    public void testWinAllColumns() {
        for (int i = 0; i < 3; i++) {
            final AtomicReference<PLAYER> wonRef = new AtomicReference<>();
            board.setCell(new Selection(0, i), PLAYER.X);
            board.setCell(new Selection(1, i), PLAYER.X);
            board.setCell(new Selection(2, i), PLAYER.X);
            assertTrue(board.won(new Selection(0, i), new Selection(1, i), new Selection(2, i), wonRef));
            assertEquals(PLAYER.X, wonRef.get());
        }
    }

    @Test
    public void testWinDiagonalsNegative() {
        final AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        board.setCell(new Selection(0, 0), PLAYER.X);
        board.setCell(new Selection(1, 1), PLAYER.X);
        board.setCell(new Selection(2, 2), PLAYER.X);
        assertTrue(board.won(new Selection(0, 0), new Selection(1, 1), new Selection(2, 2), wonRef));
        assertEquals(PLAYER.X, wonRef.get());
    }

    @Test
    public void testWinDiagonalsPositive() {
        final AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        board.setCell(new Selection(0, 2), PLAYER.X);
        board.setCell(new Selection(1, 1), PLAYER.X);
        board.setCell(new Selection(2, 0), PLAYER.X);
        assertTrue(board.won(new Selection(0, 2), new Selection(1, 1), new Selection(2, 0), wonRef));
        assertEquals(PLAYER.X, wonRef.get());
    }

    @Test
    public void testWinFalse() {
        final AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        board.setCell(new Selection(0, 0), PLAYER.X);
        board.setCell(new Selection(1, 0), PLAYER.X);
        board.setCell(new Selection(0, 2), PLAYER.X);
        assertFalse(board.won(new Selection(0, 0), new Selection(0, 1), new Selection(0, 2), wonRef));
        assertNull(wonRef.get());
    }

    @Test
    public void testWinOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> board.setCell(new Selection(3, 3), PLAYER.X));
    }
}
