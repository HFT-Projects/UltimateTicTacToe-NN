package uttt.board;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import uttt.actor.PLAYER;

public class GlobalBoardTest {
    GlobalBoard globalBoardX;
    GlobalBoard globalBoardO;
    GlobalBoard globalBoardTied;

    @BeforeEach
    public void setup() {
        globalBoardX = new GlobalBoard();
        for (int i = 0; i < 5; i++) {
            globalBoardX.getCell(i).setCell(0, PLAYER.X);
            globalBoardX.getCell(i).setCell(1, PLAYER.X);
            globalBoardX.getCell(i).setCell(2, PLAYER.X);
        }

        globalBoardO = new GlobalBoard();
        for (int i = 0; i < 5; i++) {
            globalBoardO.getCell(i).setCell(0, PLAYER.O);
            globalBoardO.getCell(i).setCell(1, PLAYER.O);
            globalBoardO.getCell(i).setCell(2, PLAYER.O);
        }

        globalBoardTied = new GlobalBoard();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                // O X O
                // X X O
                // O O X
                if (i == 1 || i == 3 || i == 4 || i == 8) {
                    globalBoardTied.getCell(i).setCell(j, PLAYER.X);
                } else {
                    globalBoardTied.getCell(i).setCell(j, PLAYER.O);
                }
            }
        }
    }

    @AfterEach
    public void teardown() {
        globalBoardX = null;
        globalBoardO = null;
        globalBoardTied = null;
    }

    @Test
    public void TestGetPlayableLocalBoardsLength() {
        assertEquals(4, globalBoardX.getPlayableLocalBoards().length);
    }

    @Test
    public void TestEndedWonX() {
        assertEquals(ENDED_STATUS.X, globalBoardX.calculateEndedStatus());
    }

    @Test
    public void TestEndedWonO() {
        assertEquals(ENDED_STATUS.O, globalBoardO.calculateEndedStatus());
    }

    @Test
    public void TestEndedNotWonX() {
        assertNotEquals(ENDED_STATUS.X, globalBoardO.calculateEndedStatus());
    }

    @Test
    public void TestEndedTied() {
        assertEquals(ENDED_STATUS.TIE, globalBoardTied.calculateEndedStatus());
    }

    @Test
    public void TestEndedNotEnded() {
        GlobalBoard gb = new GlobalBoard();
        assertNull(gb.calculateEndedStatus());
    }
}
