package helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import uttt.board.Selection;

public class HelperTest {
    @Test
    public void testSelectionToInt() {
        Selection sel = new Selection(2, 1);
        int result = Utils.selectionToInt(sel);
        assertEquals(7, result);
    }
}
