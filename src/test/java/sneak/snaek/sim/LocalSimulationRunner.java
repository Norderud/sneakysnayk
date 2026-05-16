package sneak.snaek.sim;

import sneak.snaek.engine.ModularSnakeEngine;
import sneak.snaek.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple in-memory simulation runner for local testing without networking.
 * Note: This is a simplified ruleset (Standard rules).
 */
public class LocalSimulationRunner {

    public static void main(String[] args) {
        int width = 11;
        int height = 11;

        // Initialize snakes
        List<BattleSnake> snakes = new ArrayList<>();
        snakes.add(createSnake("Snake-A", new Coord(1, 1)));
        snakes.add(createSnake("Snake-B", new Coord(9, 9)));

        Board board = new Board(height, width, List.of(new Coord(5, 5)), snakes, List.of());
        RuleSettings settings = new RuleSettings(15, 1, 14, new Royalty(0), new Squad(false, false, false, false));
        Game game = new Game("sim-game", new Ruleset("standard", "v1.2.3", settings), 500);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();

        for (int turn = 0; turn < 100; turn++) {
            System.out.println("--- Turn " + turn + " ---");
            List<Move> moves = new ArrayList<>();
            for (BattleSnake snake : board.snakes()) {
                GameState state = new GameState(game, turn, board, snake);
                Move move = engine.move(state);
                moves.add(move);
                System.out.println(snake.name() + " moves " + move);
            }

            // Update board (Very simplified: move heads, don't handle collisions/food properly yet)
            board = nextState(board, moves);
            
            if (board.snakes().isEmpty()) {
                System.out.println("All snakes died!");
                break;
            }
        }
    }

    private static BattleSnake createSnake(String name, Coord head) {
        List<Coord> body = List.of(head, head, head); // Start with length 3
        return new BattleSnake(name, name, 100, body, "0", head, 3, "", "", null);
    }

    private static Board nextState(Board board, List<Move> moves) {
        List<BattleSnake> nextSnakes = new ArrayList<>();
        for (int i = 0; i < board.snakes().size(); i++) {
            BattleSnake snake = board.snakes().get(i);
            Move move = moves.get(i);
            Coord nextHead = neighbor(snake.head(), move);

            // Simple OOB check
            if (nextHead.x() < 0 || nextHead.x() >= board.width() || nextHead.y() < 0 || nextHead.y() >= board.height()) {
                System.out.println(snake.name() + " hit a wall!");
                continue;
            }

            List<Coord> nextBody = new ArrayList<>();
            nextBody.add(nextHead);
            for (int j = 0; j < snake.body().size() - 1; j++) {
                nextBody.add(snake.body().get(j));
            }

            nextSnakes.add(new BattleSnake(snake.id(), snake.name(), snake.health() - 1, nextBody, "0", nextHead, snake.length(), "", "", null));
        }
        return new Board(board.height(), board.width(), board.food(), nextSnakes, board.hazards());
    }

    private static Coord neighbor(Coord c, Move m) {
        return switch (m) {
            case UP -> new Coord(c.x(), c.y() + 1);
            case DOWN -> new Coord(c.x(), c.y() - 1);
            case LEFT -> new Coord(c.x() - 1, c.y());
            case RIGHT -> new Coord(c.x() + 1, c.y());
        };
    }
}
