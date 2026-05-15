package sneak.snaek;
import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.CoordUtils;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;
import sneak.snaek.model.GameState;
import sneak.snaek.model.Move;
import sneak.snaek.strategy.Bfs;
import sneak.snaek.strategy.MoveScorer;
import org.slf4j.Logger;
import java.util.*;
import static org.slf4j.LoggerFactory.getLogger;
import static sneak.snaek.model.Move.UP;
/**
 * Simple, fast decision engine -- one-ply greedy.
 *
 *   1. Build a safety filter (avoid OOB / bodies / losing H2H squares).
 *   2. Score each remaining move via {@link MoveScorer}
 *      (flood-fill survival + food-race bonus).
 *   3. Pick the highest scorer.
 */
public class SnakeEngine {
    private static final Logger log = getLogger(SnakeEngine.class);
    private static final Random RANDOM = new Random();
    public Move move(GameState game) {
        long start = System.nanoTime();
        BattleSnake you  = game.you();
        BoardGrid    grid = new BoardGrid(game);
        List<BattleSnake> enemies = game.board().snakes().stream()
                .filter(s -> !s.id().equals(you.id()))
                .toList();
        // Step 1: safety filter
        PathOptions safety = new PathOptions(grid)
                .avoidBlocked(you.head())
                .avoidHeadToHeadLosses(you.head(), you.length(), enemies);
        Set<Move> safe = safety.validMoves;
        if (safe.isEmpty()) {
            log.warn("No safe moves! Returning UP as last resort.");
            return UP;
        }
        if (safe.size() == 1) {
            Move only = safe.iterator().next();
            log.info("Single safe move: {}", only);
            return only;
        }
        // Step 2: hoist per-turn computations (identical for every candidate)
        Set<Coord> food = new HashSet<>(
                game.board().food() != null ? game.board().food() : List.of());
        Bfs.EnemyReach enemyReach = Bfs.computeEnemyReach(grid, enemies);
        // Hazard damages whenever ANY body segment is inside, not just
        // the head — so even if the head is currently safe, a stretched-
        // out body in sauce is bleeding HP every turn and food urgency
        // should already be elevated.
        boolean inHazard = false;
        for (Coord seg : you.body()) {
            if (grid.isHazard(seg)) { inHazard = true; break; }
        }
        double foodWeight = MoveScorer.computeFoodWeight(
                you.length(), you.health(), inHazard, enemies);
        Coord myTail = you.body().getLast();

        // Step 3: score & pick best
        Move best = UP;
        MoveScorer.Score bestScore = null;
        for (Move m : safe) {
            MoveScorer.Score s = MoveScorer.score(grid, you.head(), you.body(), you.length(), you.health(), myTail,
                    m, enemies, food, enemyReach, foodWeight);
            log.debug("  candidate {} -> {}", m, s);
            if (bestScore == null || s.total() > bestScore.total()) {
                bestScore = s;
                best      = m;
            }
        }
        log.info("[{}] Turn {} | Head={} Health={} | Move={} {} ({}ms)",
                game.game().id(), game.turn(), you.head(), you.health(),
                best, bestScore, (System.nanoTime() - start) / 1_000_000);
        return best;
    }
    // PathOptions -- collision-safe move filter
    static class PathOptions {
        Set<Move> validMoves = new HashSet<>(Set.of(Move.values()));
        private final BoardGrid grid;
        PathOptions(BoardGrid grid) {
            this.grid = grid;
        }
        public PathOptions avoidBlocked(Coord head) {
            validMoves.removeIf(m -> grid.isBlocked(CoordUtils.neighbor(head, m)));
            return this;
        }
        public PathOptions avoidHeadToHeadLosses(Coord head, int myLength,
                                                  List<BattleSnake> enemies) {
            Set<Coord> danger = new HashSet<>();
            for (BattleSnake enemy : enemies) {
                if (enemy.length() >= myLength) {
                    for (Move m : Move.values()) {
                        danger.add(CoordUtils.neighbor(enemy.head(), m));
                    }
                }
            }
            Set<Move> filtered = new HashSet<>(validMoves);
            filtered.removeIf(m -> danger.contains(CoordUtils.neighbor(head, m)));
            if (!filtered.isEmpty()) validMoves = filtered;
            return this;
        }
        public Move randomMove() {
            if (validMoves.isEmpty()) return UP;
            List<Move> list = new ArrayList<>(validMoves);
            return list.get(RANDOM.nextInt(list.size()));
        }
    }
}