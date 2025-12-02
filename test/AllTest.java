import helper.HelperTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import uttt.board.GlobalBoardTest;
import uttt.board.LocalBoardTest;

@Suite
@SelectClasses({
        LocalBoardTest.class,
        GlobalBoardTest.class,
        HelperTest.class})
public class AllTest {
}
