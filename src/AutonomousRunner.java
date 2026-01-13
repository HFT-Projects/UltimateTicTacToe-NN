import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.Utils;
import nn.FFN;
import nn.activation.IdentityFunction;
import nn.trainer.FFNTrainer;
import nn.trainer.FFNTrainerBGD;
import nn.trainer.FFNTrainerMBGD;
import nn.trainer.FFNTrainerSGD;
import uttt.Game;
import uttt.actor.Actor;
import uttt.actor.DFSActor;
import uttt.actor.NNActor;
import uttt.actor.PLAYER;

@SuppressWarnings("DuplicatedCode")
private static final class ConfigManager {
    private ConfigManager() {
    }

    /**
     * Reads the JSON file at 'path' and parses it into a Config object.
     * Expects the JSON structure as described (mode == NN_VS_NN || NN_VS_ALGO).
     */
    public static Config parseConfig(String path) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(path)));
        ObjectMapper mapper = new ObjectMapper();
        // ignore unknown fields in the JSON (more robust)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Config cfg;
        try {
            cfg = mapper.readValue(json, Config.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }

        validateConfig(cfg);
        return cfg;
    }

    private static void validateConfig(Config cfg) {
        Objects.requireNonNull(cfg, "Config is null");
        if (cfg.mode == null) throw new IllegalArgumentException("mode is missing in config");
        if (!cfg.mode.equals("NN_VS_NN") && !cfg.mode.equals("NN_VS_ALGO"))
            throw new IllegalArgumentException("Invalid mode: " + cfg.mode);

        if (cfg.print_interval != null && cfg.print_interval <= 0)
            throw new IllegalArgumentException("print_interval is invalid");
        if (cfg.swap_actors_between_epochs == null)
            throw new IllegalArgumentException("swap_actors_between_epochs is missing");

        if (cfg.output_path == null) throw new IllegalArgumentException("output_path is missing");
        Path currentDir = Paths.get("").toAbsolutePath();
        Path folder = currentDir.resolve(cfg.output_path);
        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create output directory: " + folder, e);
            }
        }

        if (cfg.checkpoint_interval_minutes != null && cfg.checkpoint_interval_minutes < 0)
            throw new IllegalArgumentException("checkpoint_interval_minutes is invalid");
        if (cfg.epoch_count == null || cfg.epoch_count <= 0)
            throw new IllegalArgumentException("epoch_count is missing or invalid");

        if (cfg.mode.equals("NN_VS_NN")) {
            if (cfg.nn_1 == null && cfg.nn_imported_1 == null)
                throw new IllegalArgumentException("nn_1 or nn_imported_1 must be given for NN_VS_NN");
            if (cfg.nn_2 == null && cfg.nn_imported_2 == null)
                throw new IllegalArgumentException("nn_2 or nn_imported_2 must be given for NN_VS_NN");
            if (cfg.nn_1 != null && cfg.nn_imported_1 != null)
                throw new IllegalArgumentException("Only one of nn_1 or nn_imported_1 must be given for NN_VS_NN");
            if (cfg.nn_2 != null && cfg.nn_imported_2 != null)
                throw new IllegalArgumentException("Only one of nn_2 or nn_imported_2 must be given for NN_VS_NN");
            if (cfg.nn_1 != null)
                validateNN(cfg.nn_1, "nn_1");
            else
                validateNNImported(cfg.nn_imported_1, "nn_imported_1");
            if (cfg.nn_2 != null)
                validateNN(cfg.nn_2, "nn_2");
            else
                validateNNImported(cfg.nn_imported_2, "nn_imported_2");
        } else { // NN_VS_ALGO
            if (cfg.nn == null && cfg.nn_imported == null)
                throw new IllegalArgumentException("nn or nn_imported must be given for NN_VS_ALGO");
            if (cfg.nn != null && cfg.nn_imported != null)
                throw new IllegalArgumentException("Only one of nn or nn_imported must be given for NN_VS_ALGO");
            if (cfg.algo == null) throw new IllegalArgumentException("algo is missing for NN_VS_ALGO");
            if (cfg.nn != null)
                validateNN(cfg.nn, "nn");
            else
                validateNNImported(cfg.nn_imported, "nn_imported");
            validateAlgo(cfg.algo);
        }
    }

    private static void validateNN(NNConfig nn, String name) {
        if (nn.hidden_layer_sizes == null || nn.hidden_layer_sizes.length == 0)
            throw new IllegalArgumentException(name + ".hidden_layer_sizes is missing or empty");
        if (nn.board_selection_multiplier == null || nn.board_selection_multiplier <= 0)
            throw new IllegalArgumentException(name + ".board_selection_multiplier is missing or invalid");
        if (nn.activation_function == null)
            throw new IllegalArgumentException(name + ".activation_function is missing");
        if (nn.loss_function == null)
            throw new IllegalArgumentException(name + ".loss_function is missing");
        if (nn.learning_rate == null || nn.learning_rate < 0)
            throw new IllegalArgumentException(name + ".learning_rate is missing or invalid");
        if (nn.learning_rate_decay == null)
            throw new IllegalArgumentException(name + ".learning_rate_decay is missing");
        if (nn.discount_factor == null || nn.discount_factor < 0)
            throw new IllegalArgumentException(name + ".discount_factor is missing or invalid");
        if (nn.exploration_rate == null || nn.exploration_rate < 0)
            throw new IllegalArgumentException(name + ".exploration_rate is missing or invalid");
        if (nn.exploration_rate_decay == null)
            throw new IllegalArgumentException(name + ".exploration_rate_decay is missing");
        if (nn.trainer == null)
            throw new IllegalArgumentException(name + ".trainer is missing");
        validateTrainer(nn.trainer, name);
    }

    private static void validateNNImported(NNImportedConfig nn, String name) {
        if (nn.path == null)
            throw new IllegalArgumentException(name + ".path is missing");
        if (!Files.exists(Paths.get(nn.path)))
            throw new IllegalArgumentException(name + ".path does not exist: " + nn.path);
        if (nn.learning_rate == null || nn.learning_rate < 0)
            throw new IllegalArgumentException(name + ".learning_rate is missing or invalid");
        if (nn.learning_rate_decay == null)
            throw new IllegalArgumentException(name + ".learning_rate_decay is missing");
        if (nn.discount_factor == null || nn.discount_factor < 0)
            throw new IllegalArgumentException(name + ".discount_factor is missing or invalid");
        if (nn.exploration_rate == null || nn.exploration_rate < 0)
            throw new IllegalArgumentException(name + ".exploration_rate is missing or invalid");
        if (nn.exploration_rate_decay == null)
            throw new IllegalArgumentException(name + ".exploration_rate_decay is missing");
        if (nn.trainer == null)
            throw new IllegalArgumentException(name + ".trainer is missing");
        validateTrainer(nn.trainer, name);
    }

    private static void validateTrainer(TrainerConfig trainer, String name) {
        if (trainer.type == null)
            throw new IllegalArgumentException(name + ".trainer.type is missing");
        if (trainer.type.equals("Mini Batch")) {
            if (trainer.mini_batch_size == null || trainer.mini_batch_size <= 0)
                throw new IllegalArgumentException(name + ".trainer.mini_batch_size is missing or invalid");
        } else {
            if (trainer.mini_batch_size != null)
                throw new IllegalArgumentException(name + ".trainer.mini_batch_size should not be set for trainer type " + trainer.type);
        }
    }

    private static void validateAlgo(AlgoConfig algo) {
        if (algo.strength_multiplier == null || algo.strength_multiplier < 0)
            throw new IllegalArgumentException("algo" + ".strength_multiplier is missing");
    }

    // --- POJOs für die JSON-Struktur ---
    public static class Config {
        public String mode;
        public String output_path;
        public Integer print_interval;
        public Integer checkpoint_interval_minutes;
        public Integer epoch_count;
        public Boolean swap_actors_between_epochs;

        // für NN_VS_NN
        public NNConfig nn_1;
        public NNConfig nn_2;
        public NNImportedConfig nn_imported_1;
        public NNImportedConfig nn_imported_2;

        // für NN_VS_ALGO
        public NNConfig nn;
        public NNImportedConfig nn_imported;
        public AlgoConfig algo;

        @Override
        public String toString() {
            return "Config{" +
                    "mode='" + mode + '\'' +
                    ", output_path='" + output_path + '\'' +
                    ", checkpoint_interval_minutes=" + checkpoint_interval_minutes +
                    ", epoch_count=" + epoch_count +
                    ", swap_actors_between_epochs=" + swap_actors_between_epochs +
                    ", nn_1=" + nn_1 +
                    ", nn_2=" + nn_2 +
                    ", nn=" + nn +
                    ", algo=" + algo +
                    '}';
        }
    }

    public static class NNConfig {
        public int[] hidden_layer_sizes;
        public Integer board_selection_multiplier;
        public String activation_function;
        public String loss_function;
        public Double learning_rate;
        public Boolean learning_rate_decay;
        public Double discount_factor;
        public Double exploration_rate;
        public Boolean exploration_rate_decay;
        public TrainerConfig trainer;
        public String bias_initializer; // possibly null
        public String weight_initializer; // possibly null

        @Override
        public String toString() {
            //noinspection SpellCheckingInspection
            return "NNConfig{" +
                    "hidden_layer_sizes=" + Arrays.toString(hidden_layer_sizes) +
                    ", board_selection_multiplier=" + board_selection_multiplier +
                    ", activation_function='" + activation_function + '\'' +
                    ", loss_function='" + loss_function + '\'' +
                    ", learning_rate=" + learning_rate +
                    ", learning_rate_decay=" + learning_rate_decay +
                    ", discount_factor=" + discount_factor +
                    ", exploration_rate=" + exploration_rate +
                    ", exploration_rate_decay=" + exploration_rate_decay +
                    ", trainer='" + trainer + '\'' +
                    ", bias_initializer='" + bias_initializer + '\'' +
                    ", weight_initializer='" + weight_initializer + '\'' +
                    '}';
        }
    }

    public static class NNImportedConfig {
        public String path;
        public Double learning_rate;
        public Boolean learning_rate_decay;
        public Double discount_factor;
        public Double exploration_rate;
        public Boolean exploration_rate_decay;
        public TrainerConfig trainer;

        @Override
        public String toString() {
            //noinspection SpellCheckingInspection
            return "NNConfig{" +
                    "path=" + path +
                    ", learning_rate=" + learning_rate +
                    ", learning_rate_decay=" + learning_rate_decay +
                    ", discount_factor=" + discount_factor +
                    ", exploration_rate=" + exploration_rate +
                    ", exploration_rate_decay=" + exploration_rate_decay +
                    ", trainer='" + trainer + '\'' +
                    '}';
        }
    }

    public static class AlgoConfig {
        public Integer strength_multiplier;

        @Override
        public String toString() {
            return "AlgoConfig{" +
                    "strength_multiplier=" + strength_multiplier +
                    '}';
        }
    }

    public static class TrainerConfig {
        public String type;
        public Integer mini_batch_size;
    }
}

void main(String[] args) throws IOException {
    LocalDateTime lastTimestamp = LocalDateTime.now();
    final LocalDateTime startTime = LocalDateTime.now();
    FFN net1, net2;
    BiFunction<PLAYER, Integer, Actor> getActor1, getActor2;
    int actor_1_wins = 0;
    int actor_2_wins = 0;
    int x_wins = 0;
    int o_wins = 0;
    int ties = 0;

    String path;
    if (args.length > 0)
        path = args[0];
    else
        path = "config.json";

    ConfigManager.Config cfg = ConfigManager.parseConfig(path);

    switch (cfg.mode) {
        case "NN_VS_NN": {
            if (cfg.nn_1 != null) {
                int[] layerSizes1 = getLayerSizes(cfg.nn_1.board_selection_multiplier, cfg.nn_1.hidden_layer_sizes);
                net1 = new FFN(
                        layerSizes1,
                        Utils.nameToActivation.get(cfg.nn_1.activation_function),
                        new IdentityFunction(),
                        Utils.nameToLoss.get(cfg.nn_1.loss_function),
                        Utils.nameToBiasInitializer.get(cfg.nn_1.bias_initializer == null ? "Random" : cfg.nn_1.bias_initializer),
                        Utils.nameToWeightInitializer.get(cfg.nn_1.weight_initializer == null ? "Random" : cfg.nn_1.weight_initializer)
                );
                FFNTrainer trainer1 = switch (cfg.nn_1.trainer.type) {
                    case "Stochastic Gradient Descent" -> new FFNTrainerSGD();
                    case "Mini Batch" -> new FFNTrainerMBGD(layerSizes1, cfg.nn_1.trainer.mini_batch_size);
                    case "Batch" -> new FFNTrainerBGD(layerSizes1);
                    default -> throw new IllegalArgumentException("Unknown trainer: " + cfg.nn_1.trainer.type);
                };
                getActor1 = (player, ep) -> new NNActor(player, net1, trainer1, Utils.calculateAlpha(cfg.nn_1.learning_rate_decay, cfg.nn_1.learning_rate, cfg.epoch_count, ep), cfg.nn_1.discount_factor, Utils.calculateEpsilon(cfg.nn_1.exploration_rate_decay, cfg.nn_1.exploration_rate, ep));
            } else {
                net1 = FFN.load(cfg.nn_imported_1.path);
                FFNTrainer trainer1 = switch (cfg.nn_imported_1.trainer.type) {
                    case "Stochastic Gradient Descent" -> new FFNTrainerSGD();
                    case "Mini Batch" -> new FFNTrainerMBGD(net1.layerSizes, cfg.nn_imported_1.trainer.mini_batch_size);
                    case "Batch" -> new FFNTrainerBGD(net1.layerSizes);
                    default -> throw new IllegalArgumentException("Unknown trainer: " + cfg.nn_imported_1.trainer.type);
                };
                getActor1 = (player, ep) -> new NNActor(player, net1, trainer1, Utils.calculateAlpha(cfg.nn_imported_1.learning_rate_decay, cfg.nn_imported_1.learning_rate, cfg.epoch_count, ep), cfg.nn_imported_1.discount_factor, Utils.calculateEpsilon(cfg.nn_imported_1.exploration_rate_decay, cfg.nn_imported_1.exploration_rate, ep));
            }

            if (cfg.nn_2 != null) {
                int[] layerSizes2 = getLayerSizes(cfg.nn_2.board_selection_multiplier, cfg.nn_2.hidden_layer_sizes);
                net2 = new FFN(
                        layerSizes2,
                        Utils.nameToActivation.get(cfg.nn_2.activation_function),
                        new IdentityFunction(),
                        Utils.nameToLoss.get(cfg.nn_2.loss_function),
                        Utils.nameToBiasInitializer.get(cfg.nn_2.bias_initializer == null ? "Random" : cfg.nn_2.bias_initializer),
                        Utils.nameToWeightInitializer.get(cfg.nn_2.weight_initializer == null ? "Random" : cfg.nn_2.weight_initializer)
                );
                FFNTrainer trainer2 = switch (cfg.nn_2.trainer.type) {
                    case "Stochastic Gradient Descent" -> new FFNTrainerSGD();
                    case "Mini Batch" -> new FFNTrainerMBGD(layerSizes2, cfg.nn_2.trainer.mini_batch_size);
                    case "Batch" -> new FFNTrainerBGD(layerSizes2);
                    default -> throw new IllegalArgumentException("Unknown trainer: " + cfg.nn_2.trainer);
                };
                getActor2 = (player, ep) -> new NNActor(player, net2, trainer2, Utils.calculateAlpha(cfg.nn_2.learning_rate_decay, cfg.nn_2.learning_rate, cfg.epoch_count, ep), cfg.nn_2.discount_factor, Utils.calculateEpsilon(cfg.nn_2.exploration_rate_decay, cfg.nn_2.exploration_rate, ep));
            } else {
                net2 = FFN.load(cfg.nn_imported_2.path);
                FFNTrainer trainer2 = switch (cfg.nn_imported_2.trainer.type) {
                    case "Stochastic Gradient Descent" -> new FFNTrainerSGD();
                    case "Mini Batch" -> new FFNTrainerMBGD(net2.layerSizes, cfg.nn_imported_2.trainer.mini_batch_size);
                    case "Batch" -> new FFNTrainerBGD(net2.layerSizes);
                    default -> throw new IllegalArgumentException("Unknown trainer: " + cfg.nn_imported_2.trainer.type);
                };
                getActor2 = (player, ep) -> new NNActor(player, net2, trainer2, Utils.calculateAlpha(cfg.nn_imported_2.learning_rate_decay, cfg.nn_imported_2.learning_rate, cfg.epoch_count, ep), cfg.nn_imported_2.discount_factor, Utils.calculateEpsilon(cfg.nn_imported_2.exploration_rate_decay, cfg.nn_imported_2.exploration_rate, ep));
            }
            break;
        }
        case "NN_VS_ALGO": {
            net2 = null;
            getActor2 = (player, _) -> new DFSActor(player, cfg.algo.strength_multiplier);

            if (cfg.nn != null) {
                int[] layerSizes = getLayerSizes(cfg.nn.board_selection_multiplier, cfg.nn.hidden_layer_sizes);
                net1 = new FFN(
                        layerSizes,
                        Utils.nameToActivation.get(cfg.nn.activation_function),
                        new IdentityFunction(),
                        Utils.nameToLoss.get(cfg.nn.loss_function),
                        Utils.nameToBiasInitializer.get(cfg.nn.bias_initializer == null ? "Random" : cfg.nn.bias_initializer),
                        Utils.nameToWeightInitializer.get(cfg.nn.weight_initializer == null ? "Random" : cfg.nn.weight_initializer)
                );
                FFNTrainer trainer = switch (cfg.nn.trainer.type) {
                    case "Stochastic Gradient Descent" -> new FFNTrainerSGD();
                    case "Mini Batch" -> new FFNTrainerMBGD(layerSizes, cfg.nn.trainer.mini_batch_size);
                    case "Batch" -> new FFNTrainerBGD(layerSizes);
                    default -> throw new IllegalArgumentException("Unknown trainer: " + cfg.nn.trainer);
                };
                getActor1 = (player, ep) -> new NNActor(player, net1, trainer, Utils.calculateAlpha(cfg.nn.learning_rate_decay, cfg.nn.learning_rate, cfg.epoch_count, ep), cfg.nn.discount_factor, Utils.calculateEpsilon(cfg.nn.exploration_rate_decay, cfg.nn.exploration_rate, ep));
            } else {
                net1 = FFN.load(cfg.nn_imported.path);
                FFNTrainer trainer = switch (cfg.nn_imported.trainer.type) {
                    case "Stochastic Gradient Descent" -> new FFNTrainerSGD();
                    case "Mini Batch" -> new FFNTrainerMBGD(net1.layerSizes, cfg.nn_imported.trainer.mini_batch_size);
                    case "Batch" -> new FFNTrainerBGD(net1.layerSizes);
                    default -> throw new IllegalArgumentException("Unknown trainer: " + cfg.nn_imported.trainer.type);
                };
                getActor1 = (player, ep) -> new NNActor(player, net1, trainer, Utils.calculateAlpha(cfg.nn_imported.learning_rate_decay, cfg.nn_imported.learning_rate, cfg.epoch_count, ep), cfg.nn_imported.discount_factor, Utils.calculateEpsilon(cfg.nn_imported.exploration_rate_decay, cfg.nn_imported.exploration_rate, ep));
            }
            break;
        }
        default:
            throw new IllegalArgumentException("Unknown mode: " + cfg.mode);
    }

    final BiFunction<PLAYER, Integer, Actor> getActor1Final = getActor1;
    final BiFunction<PLAYER, Integer, Actor> getActor2Final = getActor2;

    for (int i = 0; i < cfg.epoch_count; i++) {
        if (Math.random() > 0.5 && cfg.swap_actors_between_epochs) {
            BiFunction<PLAYER, Integer, Actor> temp = getActor1;
            getActor1 = getActor2;
            getActor2 = temp;
        }

        Actor actorX = getActor1.apply(PLAYER.X, i);
        Actor actorO = getActor2.apply(PLAYER.O, i);

        Game game = new Game(actorX, actorO);
        if (actorX instanceof NNActor)
            game.addObserver(((NNActor) actorX)::eventHandler);
        if (actorO instanceof NNActor)
            game.addObserver(((NNActor) actorO)::eventHandler);
        switch (game.run()) {
            case X -> {
                x_wins++;
                if (getActor1Final == getActor1)
                    actor_1_wins++;
                else
                    actor_2_wins++;
            }
            case O -> {
                o_wins++;
                if (getActor2Final == getActor2)
                    actor_2_wins++;
                else
                    actor_1_wins++;
            }
            case TIE -> ties++;
        }

        if (cfg.print_interval != null && ((i + 1) % cfg.print_interval == 0 || i == cfg.epoch_count - 1)) {
            System.out.printf("Done with %.2f%% (%d/%d)%n", ((double) (i + 1) / cfg.epoch_count) * 100, i + 1, cfg.epoch_count);
            Duration d = Duration.between(startTime, LocalDateTime.now());
            double secondsPerRun = (double) d.getSeconds() / (i + 1);
            System.out.println("Prognosis: Done at " + startTime.plusSeconds((long) (secondsPerRun * cfg.epoch_count)));
            System.out.println("X Wins: " + x_wins);
            System.out.println("O Wins: " + o_wins);
            System.out.println("Ties: " + ties);
            System.out.println("Actor 1 Wins: " + actor_1_wins);
            System.out.println("Actor 2 Wins: " + actor_2_wins);
        }

        if (cfg.checkpoint_interval_minutes != null && lastTimestamp.plusMinutes(cfg.checkpoint_interval_minutes).isBefore(LocalDateTime.now())) {
            lastTimestamp = LocalDateTime.now();
            System.out.println("Saving checkpoint...");
            saveCheckpoint(net1, lastTimestamp, i, "net-x", cfg.output_path);
            if (net2 != null)
                saveCheckpoint(net2, lastTimestamp, i, "net-o", cfg.output_path);
        }
    }

    saveCheckpoint(net1, lastTimestamp, cfg.epoch_count, "net-x", cfg.output_path);
    if (net2 != null)
        saveCheckpoint(net2, lastTimestamp, cfg.epoch_count, "net-o", cfg.output_path);
}

private int[] getLayerSizes(int boardSelectionMultiplier, int[] hiddenLayerSizes) {
    final int inputSize = 18 * 9 + 9 * boardSelectionMultiplier;
    final int outputSize = 9;
    List<Integer> hiddenSizes = new LinkedList<>(Arrays.stream(hiddenLayerSizes).boxed().toList());
    hiddenSizes.addFirst(inputSize);
    hiddenSizes.add(outputSize);
    return hiddenSizes.stream().mapToInt(v -> v).toArray();
}

private static void saveCheckpoint(FFN net, LocalDateTime timestamp, int episodes, String name, String path) {
    Path currentDir = Paths.get("").toAbsolutePath();
    Path folder = currentDir.resolve(path);
    if (!Files.exists(folder)) {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + folder, e);
        }
    }
    Path filePath = folder.resolve((timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "-" + name + "-" + episodes + ".json").replace(":", "_"));
    net.save(filePath.toString());
    System.out.println("Saved checkpoint under: " + filePath);
}