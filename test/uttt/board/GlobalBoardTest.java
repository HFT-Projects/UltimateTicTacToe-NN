package uttt.board;

import helper.Utils;
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
            Selection sel = Utils.intToSelection(i);
            globalBoardX.getCell(sel).setCell(new Selection(0, 0), PLAYER.X);
            globalBoardX.getCell(sel).setCell(new Selection(0, 1), PLAYER.X);
            globalBoardX.getCell(sel).setCell(new Selection(0, 2), PLAYER.X);
        }

        globalBoardO = new GlobalBoard();
        for (int i = 0; i < 5; i++) {
            Selection sel = Utils.intToSelection(i);
            globalBoardO.getCell(sel).setCell(new Selection(0, 0), PLAYER.O);
            globalBoardO.getCell(sel).setCell(new Selection(0, 1), PLAYER.O);
            globalBoardO.getCell(sel).setCell(new Selection(0, 2), PLAYER.O);
        }

        globalBoardTied = new GlobalBoard();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                // O X O
                // X X O
                // O O X
                Selection selO = Utils.intToSelection(i);
                Selection selI = Utils.intToSelection(j);
                if (i == 1 || i == 3 || i == 4 || i == 8) {
                    globalBoardTied.getCell(selO).setCell(selI, PLAYER.X);
                } else {
                    globalBoardTied.getCell(selO).setCell(selI, PLAYER.O);
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
