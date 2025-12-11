import helper.Stats;
import nn.FFN;
import nn.activation.ActivationFunction;
import nn.activation.IdentityFunction;
import nn.activation.SigmoidFunction;
import nn.loss.LossFunction;
import nn.loss.MeanSquaredError;
import nn.trainer.FFNTrainerSGD;
import uttt.Game;
import uttt.actor.NNActor;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

static final int EPISODES = 1000;

// repeating board selection inside the input to increase weight of this part
private static final int STATE_BOARD_SELECTION_MULTIPLIER = 3;

private static final int INPUT_SIZE = 18 * 9 + 9 * STATE_BOARD_SELECTION_MULTIPLIER;
private static final int OUTPUT_SIZE = 9;
private static final int[] LAYER_SIZES = {INPUT_SIZE, 512, OUTPUT_SIZE};
public static final ActivationFunction HIDDEN_ACTIVATIONS = new SigmoidFunction();
private static final ActivationFunction OUTPUT_ACTIVATION = new IdentityFunction(); // SHOULD NOT BE CHANGED
private static final LossFunction LOSS_FUNCTION = new MeanSquaredError();

private static final double ALPHA = 0.09;
private static final double GAMMA = 0.9;
private static final double EPSILON = 0.2;

static NNActor actorX = new NNActor(PLAYER.X, new FFN(LAYER_SIZES, HIDDEN_ACTIVATIONS, OUTPUT_ACTIVATION, LOSS_FUNCTION), new FFNTrainerSGD(), ALPHA, GAMMA, EPSILON, STATE_BOARD_SELECTION_MULTIPLIER);
static NNActor actorO = new NNActor(PLAYER.O, new FFN(LAYER_SIZES, HIDDEN_ACTIVATIONS, OUTPUT_ACTIVATION, LOSS_FUNCTION), new FFNTrainerSGD(), ALPHA, GAMMA, EPSILON, STATE_BOARD_SELECTION_MULTIPLIER);

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

    stats.logAllEpisodesEnd(HIDDEN_ACTIVATIONS);
}

private static void runEpisode(Stats stats) {
    Game game = new Game(actorX, actorO);
    game.addObserver(actorX::eventHandler);
    game.addObserver(actorO::eventHandler);
    ENDED_STATUS end = game.run();
    stats.logGameEnd(end);
}
