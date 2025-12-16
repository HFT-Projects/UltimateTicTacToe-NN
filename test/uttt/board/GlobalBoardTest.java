package uttt.board;

import helper.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

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
    }

    @Test
    public void TestGetPlayableLocalBoardsLength() {
        assertEquals(4, globalBoardX.getPlayableLocalBoards().length);
    }

    @Test
    public void TestWonX() {
        final AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        assertTrue(globalBoardX.won(new Selection(0, 0), new Selection(0, 1), new Selection(0, 2), wonRef));
        assertEquals(PLAYER.X, wonRef.get());
    }

    @Test
    public void TestWonO() {
        final AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        assertTrue(globalBoardO.won(new Selection(0, 0), new Selection(0, 1), new Selection(0, 2), wonRef));
        assertEquals(PLAYER.O, wonRef.get());
    }

    @Test
    public void TestNotWon() {
        final AtomicReference<PLAYER> wonRef = new AtomicReference<>();
        assertFalse(globalBoardX.won(new Selection(0, 0), new Selection(0, 1), new Selection(2, 2), wonRef));
        assertNull(wonRef.get());
    }

    @Test
    public void testTied() {
        assertFalse(globalBoardX.tied());
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Selection selO = Utils.intToSelection(i);
                Selection selI = Utils.intToSelection(j);
                if (globalBoardX.getCell(selO).getCell(selI) == null) {
                    if ((i + j) % 2 == 0) {
                        globalBoardX.getCell(selO).setCell(selI, PLAYER.X);
                    } else {
                        globalBoardX.getCell(selO).setCell(selI, PLAYER.O);
                    }
                }
            }
        }
        assertTrue(globalBoardX.tied());
    }

    @Test
    public void TestEndedWonX() {
        assertEquals(ENDED_STATUS.X, globalBoardX.ended());
    }

    @Test
    public void TestEndedWonO() {
        assertEquals(ENDED_STATUS.O, globalBoardO.ended());
    }

    @Test
    public void TestEndedNotWonX() {
        assertNotEquals(ENDED_STATUS.X, globalBoardO.ended());
    }

    @Test
    public void TestEndedTied() {
        assertEquals(ENDED_STATUS.TIE, globalBoardTied.ended());
    }

    @Test
    public void TestEndedNotEnded() {
        GlobalBoard gb = new GlobalBoard();
        assertNull(gb.ended());
    }
}
