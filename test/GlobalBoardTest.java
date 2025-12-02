import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uttt.Board;
import uttt.GlobalBoard;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalBoardTest {
    GlobalBoard globalBoard ;

    @BeforeEach
    public void setup() {
        globalBoard = new GlobalBoard();

        for (int i = 0; i < 5; i++) {
            globalBoard.getCell(i).setCell(0, uttt.Board.PLAYER.X);
            globalBoard.getCell(i).setCell(1, uttt.Board.PLAYER.X);
            globalBoard.getCell(i).setCell(2, uttt.Board.PLAYER.X);
        }
    }
    @AfterEach
    public void teardown() {
        globalBoard = null;
    }
    @Test
    public void TestGetRemainingLocalBoardsLength() {
        assertEquals(4, globalBoard.getRemainingLocalBoards().length);
    }

    @Test
    public void TestGetRemainingLocalBoards() {
        assertEquals(globalBoard.getRemainingLocalBoard(globalBoard.getCell(8)), globalBoard.getCell(8));
    }
    @Test
    public void TestGetNotSameBoard() {
        assertNotSame(globalBoard.getRemainingLocalBoard(globalBoard.getCell(2)), globalBoard.getCell(2));
    }
    @Test
    public void TestWonSimple() {
        final AtomicReference<Board.PLAYER> wonRef = new AtomicReference<>();
        assertTrue(globalBoard.won(0,0,0,1,0,2,wonRef));
    }
    @Test
    public void TestNotWonSimple() {
        final AtomicReference<Board.PLAYER> wonRef = new AtomicReference<>();
        assertFalse(globalBoard.won(0,0,0,1,2,2,wonRef));
    }
    @Test
    public void testTied(){
        assertFalse(globalBoard.tied());
        for(int i=0; i<9; i++){
            for(int j=0; j<9; j++){
                if(globalBoard.getCell(i).getCell(j) == null){
                    if((i+j)%2==0){
                        globalBoard.getCell(i).setCell(j, Board.PLAYER.X);
                    } else {
                        globalBoard.getCell(i).setCell(j, Board.PLAYER.O);
                    }
                }
            }
        }
        assertTrue(globalBoard.tied());
    }
    @Test
    public void getStateDoubleTest(){
        double[] stateDouble = globalBoard.getStateDouble();
        boolean[] stateBoolean = globalBoard.getState();
        assertEquals(stateBoolean.length, stateDouble.length);
        for(int i=0; i<stateBoolean.length; i++){
            if(stateBoolean[i]){
                assertEquals(1.0, stateDouble[i]);
            } else {
                assertEquals(0.0, stateDouble[i]);
            }
        }
    }



}
