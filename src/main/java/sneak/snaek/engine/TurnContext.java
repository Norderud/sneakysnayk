package sneak.snaek.engine;

import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.CoordUtils;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;
import sneak.snaek.model.GameState;
import sneak.snaek.model.Move;
import sneak.snaek.strategy.Bfs;
import sneak.snaek.engine.scorer.MoveScorer;
import sneak.snaek.strategy.SurvivalArea;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Context for a single turn, holding pre-calculated data used by filters and scorers.
 */
public record TurnContext(
        GameState state,
        BoardGrid grid,
        List<BattleSnake> enemies,
        Set<Coord> food,
        Bfs.EnemyReach enemyReach,
        double foodWeight,
        Coord myTail,
        long startTimeNanos
) {
    public static TurnContext from(GameState state) {
        long start = System.nanoTime();
        BoardGrid grid = new BoardGrid(state);
        List<BattleSnake> enemies = state.board().snakes().stream()
                .filter(s -> !s.id().equals(state.you().id()))
                .toList();
        
        Set<Coord> food = new HashSet<>(
                state.board().food() != null ? state.board().food() : List.of());
        
        Bfs.EnemyReach enemyReach = Bfs.computeEnemyReach(grid, enemies);
        
        boolean inHazard = false;
        for (Coord seg : state.you().body()) {
            if (grid.isHazard(seg)) {
                inHazard = true;
                break;
            }
        }
        
        double foodWeight = MoveScorer.computeFoodWeight(
                state.you().length(), state.you().health(), inHazard, enemies);
        
        Coord myTail = state.you().body().getLast();
        
        return new TurnContext(state, grid, enemies, food, enemyReach, foodWeight, myTail, start);
    }
    
    public long elapsedMillis() {
        return (System.nanoTime() - startTimeNanos) / 1_000_000;
    }
    
    public MoveContext createMoveContext(Move move) {
        Coord next = CoordUtils.neighbor(state.you().head(), move);
        int[][] myDist = Bfs.from(grid, List.of(next));
        
        int tailDist = 0;
        if (myTail.x() >= 0 && myTail.x() < grid.getWidth() && myTail.y() >= 0 && myTail.y() < grid.getHeight()) {
            tailDist = myDist[myTail.x()][myTail.y()];
        } else {
            tailDist = Bfs.UNREACHABLE;
        }
        boolean canReachTail = tailDist < Bfs.UNREACHABLE;

        SurvivalArea.Area owned = SurvivalArea.compute(myDist, enemyReach, enemies, state.you().length(), grid);
        // Trap detection: we are "trapped" if we lack enough physical room for our body (floodCount),
        // OR if the enemy "owns" enough space to squeeze us below our body length (rawCount).
        // This second condition (rawCount < length) triggers much earlier and avoids being
        // slowly lured into corners or pressured against walls.
        boolean trapped = owned.floodCount() < state.you().length() || owned.rawCount() < state.you().length();

        return new MoveContext(this, move, next, myDist, owned, trapped, canReachTail, tailDist);
    }
}
