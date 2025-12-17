package helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

public class HelperTest {
    // Helper functions for test boards
    private PLAYER[] makeWinningBoard(PLAYER p) {
        PLAYER[] b = new PLAYER[9];
        // Win in the first row
        b[0] = p;
        b[1] = p;
        b[2] = p;
        return b;
    }

    private PLAYER[] makeTieBoard() {
        PLAYER[] b = new PLAYER[9];
        // Fully occupied but without three-in-a-row (no winner)
        b[0] = PLAYER.X;
        b[1] = PLAYER.O;
        b[2] = PLAYER.X;
        b[3] = PLAYER.X;
        b[4] = PLAYER.O;
        b[5] = PLAYER.O;
        b[6] = PLAYER.O;
        b[7] = PLAYER.X;
        b[8] = PLAYER.X;
        return b;
    }

    private PLAYER[] makeOngoingBoard() {
        PLAYER[] b = new PLAYER[9];
        // Empty cell -> ongoing game
        b[0] = PLAYER.X;
        b[1] = null;
        b[2] = PLAYER.O;
        return b;
    }

    @Test
    public void testLocalEndWinTieOngoing() {
        // X wins locally
        PLAYER[] winX = makeWinningBoard(PLAYER.X);
        ENDED_STATUS resWin = Utils.localEnded(winX);
        assertEquals(ENDED_STATUS.X, resWin);

        // Local tie
        PLAYER[] tie = makeTieBoard();
        ENDED_STATUS resTie = Utils.localEnded(tie);
        assertEquals(ENDED_STATUS.TIE, resTie);

        // Local ongoing game
        PLAYER[] ongoing = makeOngoingBoard();
        ENDED_STATUS resOngoing = Utils.localEnded(ongoing);
        assertNull(resOngoing);
    }

    @Test
    public void testGlobalEndWinAndTie() {
        // global: first three local boards are O-winners -> global O win
        PLAYER[][] global = new PLAYER[9][];
        PLAYER[] winX = makeWinningBoard(PLAYER.O);
        for (int i = 0; i < 3; i++) global[i] = winX;
        for (int i = 3; i < 9; i++) global[i] = new PLAYER[9]; // ongoing/empty
        ENDED_STATUS gRes = Utils.globalEnded(global);
        assertEquals(ENDED_STATUS.O, gRes);

        // global: all local boards are TIE -> global TIE
        PLAYER[][] allTie = new PLAYER[9][];
        PLAYER[] tie = makeTieBoard();
        for (int i = 0; i < 9; i++) allTie[i] = tie;
        ENDED_STATUS gTie = Utils.globalEnded(allTie);
        assertEquals(ENDED_STATUS.TIE, gTie);
    }

    @Test
    public void testEndedStatusPlayerMappings() {
        // ENDED_STATUS -> PLAYER
        assertEquals(PLAYER.X, Utils.ENDED_STATUS_TO_PLAYER.get(ENDED_STATUS.X));
        assertEquals(PLAYER.O, Utils.ENDED_STATUS_TO_PLAYER.get(ENDED_STATUS.O));
        // TIE maps to null (no player)
        assertNull(Utils.ENDED_STATUS_TO_PLAYER.get(ENDED_STATUS.TIE));
        // null remains null
        assertNull(Utils.ENDED_STATUS_TO_PLAYER.get(null));

        // PLAYER -> ENDED_STATUS
        assertEquals(ENDED_STATUS.X, Utils.PLAYER_TO_ENDED_STATUS.get(PLAYER.X));
        assertEquals(ENDED_STATUS.O, Utils.PLAYER_TO_ENDED_STATUS.get(PLAYER.O));
        // null remains null
        assertNull(Utils.PLAYER_TO_ENDED_STATUS.get(null));
    }

    @Test
    public void testActivationAndLossMappingsRoundTrip() {
        assertTrue(Utils.nameToActivation.containsKey("ReLU"));
        assertTrue(Utils.nameToActivation.containsKey("ELU"));
        String reluName = Utils.activationToName.get(Utils.nameToActivation.get("ReLU"));
        assertEquals("ReLU", reluName);

        // Loss mapping
        assertTrue(Utils.nameToLoss.containsKey("MSE"));
        String mseName = Utils.lossToName.get(Utils.nameToLoss.get("MSE"));
        assertEquals("MSE", mseName);
    }
}
