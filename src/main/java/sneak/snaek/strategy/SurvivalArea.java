package sneak.snaek.strategy;

import sneak.snaek.board.BoardGrid;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;

import java.util.List;

/**
 * Voronoi-weighted survival area calculation.
 *
 * Given our BFS distance grid from a candidate cell, counts how many cells
 * we win the race to vs all enemies. A cell is "owned" iff:
 *   - we reach it strictly faster than any enemy, OR
 *   - we tie with the closest enemy AND we are strictly longer (we'd win
 *     a head-to-head if both arrived simultaneously).
 *
 * Hazard cells contribute {@link ScoringConstants#HAZARD_AREA_WEIGHT}
 * (instead of 1.0) to the weighted total — they're survivable but drain
 * HP, so a pocket built mostly of hazard is effectively smaller.
 *
 * In solo (no enemies), every reachable cell is owned — this collapses
 * to a plain flood-fill.
 */
public final class SurvivalArea {

    /** Outcome of {@link #compute}.
     *  - {@code rawCount}  — Voronoi-claimed cells (we win the race).
     *  - {@code weighted}  — same, but hazards count at HAZARD_AREA_WEIGHT.
     *  - {@code floodCount}— total cells reachable by us via BFS, regardless
     *    of who wins the race. Used for **physical** trap detection; a
     *    small Voronoi count expresses enemy pressure but doesn't
     *    necessarily mean we lack room to fit our body. */
    public record Area(int rawCount, double weighted, int floodCount) {}

    private SurvivalArea() {}

    public static Area compute(int[][] myDist,
                               Bfs.EnemyReach enemyReach,
                               List<BattleSnake> enemies,
                               int myLength,
                               BoardGrid grid) {
        int[][] enemyDist  = enemyReach.dist();
        int[][] enemyOwner = enemyReach.owner();
        int    raw      = 0;
        int    flood    = 0;
        double weighted = 0.0;
        for (int x = 0; x < myDist.length; x++) {
            for (int y = 0; y < myDist[0].length; y++) {
                int md = myDist[x][y];
                if (md >= Bfs.UNREACHABLE) continue;
                flood++;
                int ed = enemyDist[x][y];
                int myArrival = md + 1;
                boolean own;
                if (myArrival < ed) {
                    own = true;
                } else if (myArrival == ed) {
                    int oi = enemyOwner[x][y];
                    own = oi >= 0 && myLength > enemies.get(oi).length();
                } else {
                    own = false;
                }
                if (own) {
                    raw++;
                    weighted += grid.isHazard(new Coord(x, y))
                            ? ScoringConstants.HAZARD_AREA_WEIGHT
                            : 1.0;
                }
            }
        }
        return new Area(raw, weighted, flood);
    }
}

