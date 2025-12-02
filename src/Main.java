import helper.Stats;
import uttt.Game;
import uttt.actor.NNActor;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

static final int EPISODES = 1000;

static NNActor actorX = new NNActor(PLAYER.X);
static NNActor actorO = new NNActor(PLAYER.O);

void main() {
    Stats stats = new Stats();

    for (int ep = 1; ep <= EPISODES; ep++) {
        runEpisode(stats);
        Stats.logEpisodeFinished(ep);

        // swap players every episode by chance
        if (Math.random() > 0.5) {
            NNActor tmp = actorX;
            actorX = actorO;
            actorO = tmp;
        }
    }

    stats.logAllEpisodesEnd(NNActor.HIDDEN_ACTIVATIONS);
}

private static void runEpisode(Stats stats) {
    Game game = new Game(actorX, actorO);
    game.addObserver(actorX.observer);
    game.addObserver(actorO.observer);
    ENDED_STATUS end = game.run();
    stats.logGameEnd(end);
}
