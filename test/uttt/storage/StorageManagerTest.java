package uttt.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

import uttt.actor.PLAYER;

public class StorageManagerTest {

    @Test
    void saveAndLoad() throws Exception {
        Move[] original = new Move[]{
                new Move(PLAYER.X, 0, 0, null, null),
                new Move(PLAYER.O, 1, 1, null, null),
                new Move(PLAYER.X, 8, 7, null, null)
        };


        @SuppressWarnings("SpellCheckingInspection")
        Path tmp = Files.createTempFile("storagemanager-test", ".json");
        try {
            StorageManager.save(original, tmp.toString());
            Move[] loaded = StorageManager.load(tmp.toString());

            assertNotNull(loaded);
            assertEquals(original.length, loaded.length, "Number of moves must match");

            // use original array and deep equals (Move is expected to be a record or properly implement equals)
            assertTrue(Arrays.deepEquals(original, loaded), "Saved and loaded moves must be equal (deep)");
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void movesToStateAndStates() {
        // ...existing code...
        Move[] moves = new Move[]{
                new Move(PLAYER.X, 0, 0, null, null),
                new Move(PLAYER.O, 0, 1, null, null),
                new Move(PLAYER.X, 4, 4, null, null)
        };

        PLAYER[][] finalState = StorageManager.movesToState(moves);
        assertNotNull(finalState);

        // build expected final state and compare with deep equals
        PLAYER[][] expectedFinal = new PLAYER[9][9];
        expectedFinal[0][0] = PLAYER.X;
        expectedFinal[0][1] = PLAYER.O;
        expectedFinal[4][4] = PLAYER.X;
        assertTrue(Arrays.deepEquals(expectedFinal, finalState), "Final state must match expected state (deep)");

        PLAYER[][][] snapshots = StorageManager.movesToStates(moves);
        // snapshots length should be moves.length + 1 (initial empty state + states after each move)
        assertEquals(moves.length + 1, snapshots.length, "Number of snapshots is incorrect");

        // build expected snapshots
        PLAYER[][][] expectedSnapshots = new PLAYER[moves.length + 1][9][9];
        // initial snapshot: all null (default)
        // after first move
        expectedSnapshots[1][0][0] = PLAYER.X;
        // after second move
        expectedSnapshots[2][0][0] = PLAYER.X;
        expectedSnapshots[2][0][1] = PLAYER.O;
        // after third move
        expectedSnapshots[3][0][0] = PLAYER.X;
        expectedSnapshots[3][0][1] = PLAYER.O;
        expectedSnapshots[3][4][4] = PLAYER.X;

        assertTrue(Arrays.deepEquals(expectedSnapshots, snapshots), "Snapshots must match expected snapshots (deep)");
    }
}
