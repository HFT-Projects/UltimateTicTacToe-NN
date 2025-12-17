package uttt.actor;

public abstract class Actor {
    private final PLAYER player;

    public Actor(PLAYER player) {
        this.player = player;
    }

    public PLAYER getPlayer() {
        return this.player;
    }

    public abstract int move(PLAYER[][] state, int localBoardSel, int[] playableActions);

    public abstract int chooseBoard(PLAYER[][] state, int[] playableBoards);
}
