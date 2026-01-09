package uttt.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.Utils;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;

import java.io.UncheckedIOException;
import java.util.*;

public final class StorageManager {
    private StorageManager() {
    }

    private static Map<String, Object> moveToMap(Move move) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        int b = move.board();
        int a = move.action();
        serialized.put("player", move.player());
        serialized.put("board", b);
        serialized.put("action", a);

        // make map immutable
        return Collections.unmodifiableMap(serialized);
    }

    private static Move mapToMove(Map<String, Object> map, PLAYER[][] state) {
        PLAYER player = switch ((String) map.get("player")) {
            case "X" -> PLAYER.X;
            case "O" -> PLAYER.O;
            default -> throw new IllegalArgumentException("Unknown player: " + map.get("player"));
        };

        int b = (Integer) map.get("board");
        if (b < 0 || b > 8)
            throw new IllegalArgumentException("Board index out of bounds: " + b);

        int a = (Integer) map.get("action");
        if (a < 0 || a > 8)
            throw new IllegalArgumentException("Action index out of bounds: " + a);

        applyMove(state, b, a, player);

        ENDED_STATUS localEndedStatus = Utils.localEnded(state[b]);
        ENDED_STATUS globalEndedStatus = null;
        // only check global ended status if local has ended
        if (localEndedStatus != null)
            globalEndedStatus = Utils.globalEnded(state);

        return new Move(player,
                b,
                a,
                localEndedStatus,
                globalEndedStatus);
    }

    public static void save(Move[] moves, String filepath) {
        try {
            List<Object> jsonO = new LinkedList<>(Arrays.stream(moves).map(StorageManager::moveToMap).toList());
            String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonO);

            try (var writer = new java.io.FileWriter(filepath)) {
                writer.write(json);
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Move[] load(String filepath) {
        try {
            String json;
            try (var reader = new java.io.FileReader(filepath)) {
                json = reader.readAllAsString();
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }

            @SuppressWarnings("unchecked")
            List<Object> list = new ObjectMapper().readValue(json, List.class);
            PLAYER[][] state = new PLAYER[9][9];
            @SuppressWarnings("unchecked")
            Move[] moves = list.stream().map(e -> (Map<String, Object>) e).map(m -> mapToMove(m, state)).toArray(Move[]::new);
            return moves;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T[][] deeperClone(T[][] source) {
        T[][] copy = source.clone();
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private static void applyMove(PLAYER[][] state, int board, int action, PLAYER player) {
        state[board][action] = player;
    }

    private static void applyMove(PLAYER[][] state, Move move) {
        applyMove(state, move.board(), move.action(), move.player());
    }

    public static PLAYER[][] movesToState(Move[] moves) {
        PLAYER[][] state = new PLAYER[9][9];
        for (Move move : moves) {
            applyMove(state, move);
        }
        return state;
    }

    public static PLAYER[][][] movesToStates(Move[] moves) {
        List<PLAYER[][]> states = new ArrayList<>();
        PLAYER[][] state = new PLAYER[9][9];
        states.add(deeperClone(state));
        for (Move move : moves) {
            applyMove(state, move);
            states.add(deeperClone(state));
        }
        return states.toArray(new PLAYER[0][][]);
    }
}
