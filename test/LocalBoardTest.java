import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uttt.Board;
import uttt.LocalBoard;


import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class LocalBoardTest {
    private LocalBoard board;

    @BeforeEach
    public void setup(){
        board = new LocalBoard(0, 0);
    }
    @AfterEach
    public void teardown(){
        board = null;
    }

    @Test
    public void getStateTest(){
        board.setCell(0, Board.PLAYER.X);
        board.setCell(5, Board.PLAYER.X);
        board.setCell(8, Board.PLAYER.O);
        boolean[] expectedState = {true, false, false, false, false, false, false, false, false, false, true,
                false, false, false, false, false, false, true};
        assertArrayEquals(board.getState(), expectedState);
    }
    @Test
    public void ThrowCellAlreadySetException(){
        board.setCell(0, Board.PLAYER.X);
        assertThrows(LocalBoard.CellAlreadySetException.class, () -> board.setCell(0, Board.PLAYER.O));
    }
    @Test
    public void getValidActionsTest(){
        int[] validActions = board.getValidActions();
        int[] expectedValidActions = IntStream.iterate(0, i -> i+1).limit(9).toArray();
        assertArrayEquals(validActions, expectedValidActions);

        board.setCell(8, Board.PLAYER.X);
        board.setCell(7, Board.PLAYER.O);
        board.setCell(6, Board.PLAYER.O);

        validActions = board.getValidActions();
        expectedValidActions = IntStream.iterate(0, i -> i+1).limit(6).toArray();
        assertArrayEquals(validActions, expectedValidActions);
    }
    @Test
    public void testTied(){
        assertFalse(board.tied());
        for(int i=0; i<9; i++){
            board.setCell(i, Board.PLAYER.O);
        }
        assertTrue(board.tied());
    }
    @Test
    public void testWinAllRows(){
        for(int i=0; i<3; i++){
        final AtomicReference<Board.PLAYER> wonRef = new AtomicReference<>();
            board.setCell(i+2*i, Board.PLAYER.X);
            board.setCell(i+1+2*i, Board.PLAYER.X);
            board.setCell(i+2+2*i, Board.PLAYER.X);
            assertTrue(board.won(i, 0, i, 1, i, 2, wonRef));
            assertEquals(Board.PLAYER.X, wonRef.get());
        }
    }
    @Test
    public void testWinAllColumns(){
        for(int i=0; i<3; i++){
        final AtomicReference<Board.PLAYER> wonRef = new AtomicReference<>();
            board.setCell(i, Board.PLAYER.X);
            board.setCell(i+3, Board.PLAYER.X);
            board.setCell(i+6, Board.PLAYER.X);
            assertTrue(board.won(0, i, 1, i, 2, i, wonRef));
            assertEquals(Board.PLAYER.X, wonRef.get());
        }
    }
    @Test
    public void testWinDiagonalsNegativ(){
        final AtomicReference<Board.PLAYER> wonRef = new AtomicReference<>();
        board.setCell(0, Board.PLAYER.X);
        board.setCell(4, Board.PLAYER.X);
        board.setCell(8, Board.PLAYER.X);
        assertTrue(board.won(0, 0, 1, 1, 2, 2, wonRef));
        assertEquals(Board.PLAYER.X, wonRef.get());
    }
    @Test
    public void testWinDiagonalsPositiv(){
        final AtomicReference<Board.PLAYER> wonRef = new AtomicReference<>();
        board.setCell(2, Board.PLAYER.X);
        board.setCell(4, Board.PLAYER.X);
        board.setCell(6, Board.PLAYER.X);
        assertTrue(board.won(0, 2, 1, 1, 2, 0, wonRef));
        assertEquals(Board.PLAYER.X, wonRef.get());
    }
    @Test
    public void testWinFalse(){
        final AtomicReference<Board.PLAYER> wonRef = new AtomicReference<>();
        board.setCell(0, Board.PLAYER.X);
        board.setCell(3, Board.PLAYER.X);
        board.setCell(2, Board.PLAYER.X);
        assertFalse(board.won(0, 0, 0, 1, 0, 2, wonRef));
        assertNull(wonRef.get());
    }
    @Test
    public void testWinOutOfBoundsException(){
        assertThrows(IndexOutOfBoundsException.class, () -> board.setCell(9, Board.PLAYER.X));
    }




}
