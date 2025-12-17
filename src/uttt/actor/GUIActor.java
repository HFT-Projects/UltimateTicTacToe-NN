package uttt.actor;

import helper.TriFunction;

import java.util.function.BiFunction;

public class GUIActor extends Actor {
    private final TriFunction<PLAYER[][], Integer, int[], Integer> moveHandler;
    private final BiFunction<PLAYER[][], int[], Integer> chooseBoardHandler;

    public GUIActor(PLAYER player, TriFunction<PLAYER[][], Integer, int[], Integer> moveHandler, BiFunction<PLAYER[][], int[], Integer> chooseBoardHandler) {
        this.moveHandler = moveHandler;
        this.chooseBoardHandler = chooseBoardHandler;
        super(player);
    }

    @Override
    public int move(PLAYER[][] state, int localBoardSel, int[] playableActions) {
        return moveHandler.apply(state, localBoardSel, playableActions);
    }

    @Override
    public int chooseBoard(PLAYER[][] state, int[] playableBoards) {
        return chooseBoardHandler.apply(state, playableBoards);
    }
}
