package uttt.actor;

import uttt.board.Selection;

public abstract class Actor {
    private final PLAYER player;

    public Actor(PLAYER player) {
        this.player = player;
    }

    public PLAYER getPlayer() {
        return this.player;
    }

    public abstract Selection move(PLAYER[][] state, Selection localBoardSel, Selection[] playableActions);

    public abstract Selection chooseBoard(PLAYER[][] state, Selection[] playableBoards);
}
