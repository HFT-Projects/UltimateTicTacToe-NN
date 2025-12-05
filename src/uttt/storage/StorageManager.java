package uttt.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uttt.actor.PLAYER;
import uttt.board.ENDED_STATUS;
import uttt.board.Selection;

import java.io.UncheckedIOException;
import java.util.*;

@SuppressWarnings("unused")
public final class StorageManager {
    private StorageManager() {
    }

    private static Map<String, Object> moveToMap(Move move) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        Selection b = move.board();
        Selection a = move.action();
        serialized.put("player", move.player());
        serialized.put("board", b.idxRow() * 3 + b.idxColumn());
        serialized.put("action", a.idxRow() * 3 + a.idxColumn());
        serialized.put("localEndedStatus", move.localEndedStatus());
        serialized.put("globalEndedStatus", move.globalEndedStatus());

        // make map immutable
        return Collections.unmodifiableMap(serialized);
    }

    private static Move mapToMove(Map<String, Object> map) {
        PLAYER player = switch ((String) map.get("player")) {
            case "X" -> PLAYER.X;
            case "O" -> PLAYER.O;
            default -> throw new IllegalArgumentException("Unknown player: " + map.get("player"));
        };

        ENDED_STATUS localEndedStatus = switch ((String) map.get("localEndedStatus")) {
            case null -> null;
            case "X" -> ENDED_STATUS.X;
            case "O" -> ENDED_STATUS.O;
            case "TIE" -> ENDED_STATUS.TIE;
            default -> throw new IllegalArgumentException("Unknown local ended status: " + map.get("localEndedStatus"));
        };

        ENDED_STATUS globalEndedStatus = switch ((String) map.get("globalEndedStatus")) {
            case null -> null;
            case "X" -> ENDED_STATUS.X;
            case "O" -> ENDED_STATUS.O;
            case "TIE" -> ENDED_STATUS.TIE;
            default ->
                    throw new IllegalArgumentException("Unknown global ended status: " + map.get("globalEndedStatus"));
        };

        int b = (Integer) map.get("board");
        if (b < 0 || b > 8)
            throw new IllegalArgumentException("Board index out of bounds: " + b);

        int a = (Integer) map.get("action");
        if (a < 0 || a > 8)
            throw new IllegalArgumentException("Action index out of bounds: " + a);

        return new Move(player,
                new Selection(b / 3, b % 3),
                new Selection(a / 3, a % 3),
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
            @SuppressWarnings("unchecked")
            Move[] moves = list.stream().map(e -> (Map<String, Object>) e).map(StorageManager::mapToMove).toArray(Move[]::new);
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

    private static void applyMove(PLAYER[][] state, Move move) {
        int boardIdx = move.board().idxRow() * 3 + move.board().idxColumn();
        int actionIdx = move.action().idxRow() * 3 + move.action().idxColumn();
        state[boardIdx][actionIdx] = move.player();
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
