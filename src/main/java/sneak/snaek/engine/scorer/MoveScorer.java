package sneak.snaek.engine.scorer;

import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.CoordUtils;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;
import sneak.snaek.model.Move;
import sneak.snaek.strategy.Bfs;
import sneak.snaek.strategy.SurvivalArea;

import java.util.List;
import java.util.Set;

/**
 * One-ply move scoring — the bot's "personality" is summed up here:
 * {@code total = survival + food + tail + stretch + h2h + centre − hazard − wall}.
 *
 * Only the orchestration lives in this class. Heavy lifting is split out:
 *   - constants    → {@link ScoringConstants}
 *   - BFS engines  → {@link Bfs}
 *   - survival/Voronoi area → {@link SurvivalArea}
 *
 * Per-turn helpers ({@link #computeFoodWeight}, {@link Bfs#computeEnemyReach})
 * are hoisted to the caller so we don't repeat identical work for each
 * candidate move.
 */
public final class MoveScorer {

    private MoveScorer() {}

    /**
     * Per-component score breakdown for a single candidate move.
     * Total = survival + food + tail + stretch + h2h + centre − hazardPenalty.
     * Diagnostic fields ({@code trapped}, {@code canReachTail},
     * {@code ownedRaw}) help explain why a move was picked when reading
     * the logs.
     */
    public record Score(double total,
                        double survival,
                        double food,
                        double tail,
                        double stretch,
                        double aggression,
                        double centre,
                        double hazardPenalty,
                        double wallPenalty,
                        boolean trapped,
                        boolean canReachTail,
                        int    ownedRaw,
                        int    flood) {
        @Override public String toString() {
            return String.format(
                "total=%.1f survival=%.1f food=%.1f tail=%.1f stretch=%.1f aggression=%.1f centre=%.1f hazard=-%.1f wall=-%.1f trapped=%s tailReach=%s owned=%d flood=%d",
                total, survival, food, tail, stretch, aggression, centre, hazardPenalty, wallPenalty, trapped, canReachTail, ownedRaw, flood);
        }
    }

    public static Score score(BoardGrid grid,
                              Coord myHead,
                              List<Coord> myBody,
                              int myLength,
                              int myHealth,
                              Coord myTail,
                              Move move,
                              List<BattleSnake> enemies,
                              Set<Coord> food,
                              Bfs.EnemyReach enemyReach,
                              double foodWeight) {

        Coord next = CoordUtils.neighbor(myHead, move);

        // Single BFS from the candidate cell — reused for survival, tail, food.
        int[][] myDist = Bfs.from(grid, List.of(next));

        int tailDist = bfsTailDistance(myDist, myTail);
        boolean canReachTail = tailDist < Bfs.UNREACHABLE;

        // 1. Survival: Voronoi-weighted area, with trap penalty when too tight.
        //    Non-trapped survival is multiplied by SURVIVAL_WEIGHT so
        //    area deltas dominate positional bonuses (otherwise a +50
        //    stretch/centre swing would routinely outweigh a 20-cell
        //    drop in owned region — exactly the trap that lured the bot
        //    into shrinking pockets next to enemies). TRAP_PENALTY stays
        //    un-scaled so the trapped/non-trapped cliff is unchanged.
        //
        //    Trap detection uses both physical room (floodCount) and
        //    contested room (rawCount). We are "trapped" if we lack
        //    enough physical room for our body, OR if the enemy "owns"
        //    enough space to squeeze us below our body length.
        //    This avoids being slowly pressured into dead-ends by enemies.
        SurvivalArea.Area owned = SurvivalArea.compute(myDist, enemyReach, enemies, myLength, grid);
        boolean trapped = owned.floodCount() < myLength || owned.rawCount() < myLength;
        double survival = trapped
                ? owned.weighted() - ScoringConstants.TRAP_PENALTY
                : owned.weighted() * ScoringConstants.SURVIVAL_WEIGHT;

        double hazardPenalty = hazardDrainPenalty(grid, next, myBody, food);
        double wallPenalty   = wallPenalty(grid, next);
        double aggressionBonus = aggressionBonus(next, enemies, myLength, trapped, owned);
        double tailBonus    = tailRescueBonus(canReachTail, tailDist, owned, myLength);
        double stretchBonus = stretchBonus(canReachTail, tailDist, owned, myLength);
        double centreBonus  = centreBonus(grid, next);
        double foodBonus    = foodBonus(myDist, enemyReach, enemies, food,
                                       foodWeight, trapped, canReachTail,
                                       owned, myLength, myHealth, grid);

        // When trapped, positional heuristics (wall, centre, tail, stretch,
        // hazard) are misleading — none of them predict who survives the
        // longest in a death pocket. Only raw remaining space matters,
        // so we zero them out and rank purely on survival + the (already
        // suppressed) food/aggression escape signals. Without this, a smaller
        // interior pocket beats a larger border one because the wall
        // penalty (−50) and centre delta easily outweigh tens of cells
        // of survival room. Zeroed in the Score record too so the log
        // line matches the arithmetic.
        if (trapped) {
            tailBonus = 0.0;
            stretchBonus = 0.0;
            centreBonus = 0.0;
            hazardPenalty = 0.0;
            wallPenalty = 0.0;
        }

        double total = survival + foodBonus + tailBonus + stretchBonus + aggressionBonus + centreBonus
                     - hazardPenalty - wallPenalty;
        return new Score(total, survival, foodBonus, tailBonus, stretchBonus, aggressionBonus,
                centreBonus, hazardPenalty, wallPenalty, trapped, canReachTail,
                owned.rawCount(), owned.floodCount());
    }

    // -----------------------------------------------------------------------
    // Per-turn helpers (compute once in SnakeEngine, reuse for every candidate)
    // -----------------------------------------------------------------------

    /**
     * Per-turn food bonus weight. Combines three signals:
     *   1. Length lead vs the longest enemy — leading snakes need food
     *      less, so the weight decays as 1/(1+excess) past LEAD_THRESHOLD.
     *      Solo play (no enemies) keeps full weight always.
     *   2. Health urgency — at HP ≤ HEALTH_URGENCY_THRESHOLD the weight
     *      ramps linearly up to 2× at HP 0. A starving long-leader
     *      still chases food.
     *   3. Hazard presence — if ANY segment of our body is currently
     *      in a hazard cell (this variant drains HP based on body
     *      presence, not just the head), we treat current HP as if it
     *      were HAZARD_HP_BUFFER lower (pre-empting the cliff).
     *
     * The three multipliers compose, so a low-HP snake stuck in hazard
     * pursues food much more aggressively than a comfortable leader.
     */
    public static double computeFoodWeight(int myLength,
                                           int myHealth,
                                           boolean inHazard,
                                           List<BattleSnake> enemies) {
        // 1. Length-lead dampener.
        double base = ScoringConstants.FOOD_BONUS;
        if (!enemies.isEmpty()) {
            int maxEnemyLen = enemies.stream().mapToInt(BattleSnake::length).max().orElse(0);
            int lead        = myLength - maxEnemyLen;
            int excess      = Math.max(0, lead - ScoringConstants.LEAD_THRESHOLD);
            base = ScoringConstants.FOOD_BONUS / (1.0 + excess);
        }

        // 2 + 3. Health/hazard urgency multiplier (ranges 1.0 → 2.0).
        int effectiveHealth = inHazard
                ? Math.max(0, myHealth - ScoringConstants.HAZARD_HP_BUFFER)
                : myHealth;
        double deficit = Math.max(0,
                ScoringConstants.HEALTH_URGENCY_THRESHOLD - effectiveHealth);
        double urgency = 1.0 + deficit / ScoringConstants.HEALTH_URGENCY_THRESHOLD;

        return base * urgency;
    }

    // -----------------------------------------------------------------------
    // Per-component scoring helpers (private — kept here while the formula
    // is small enough to read top-to-bottom; promote to dedicated classes
    // if any term grows beyond ~20 lines).
    // -----------------------------------------------------------------------

    /** BFS distance from candidate to our tail, falling back to nearest
     *  reachable neighbour of the tail when the tail itself is still
     *  blocked (recent eat → won't vacate this turn). */
    public static int bfsTailDistance(int[][] myDist, Coord myTail) {
        int tailDist = Bfs.at(myDist, myTail);
        if (tailDist >= Bfs.UNREACHABLE) {
            for (Coord n : CoordUtils.neighbors(myTail)) {
                int v = Bfs.at(myDist, n);
                if (v < tailDist) tailDist = v;
            }
        }
        return tailDist;
    }

    /** Aggression: bonus for moves that threaten or trap enemies.
     *  Combines immediate head-to-head threats and Voronoi trapping.
     *  Skipped when trapped or with no room to retreat.
     *
     *  Bonus magnitude is divided by max(1, enemies.size()): in 1v1 the
     *  full bonuses fire (high-value contest), but in a crowded
     *  Battle Royale it scales down so we don't commit to a fight that
     *  a third snake can capitalise on. */
    public static double aggressionBonus(Coord next,
                                          List<BattleSnake> enemies,
                                          int myLength,
                                          boolean trapped,
                                          SurvivalArea.Area owned) {
        if (trapped || owned.floodCount() < myLength) return 0.0;

        double bonus = 0.0;
        int[] enemyAreas = owned.enemyRawCounts();

        for (int i = 0; i < enemies.size(); i++) {
            BattleSnake enemy = enemies.get(i);

            // 1. Head-to-head threat: we are adjacent to a shorter enemy's head.
            if (enemy.length() < myLength && CoordUtils.manhattanDistance(next, enemy.head()) == 1) {
                bonus += ScoringConstants.H2H_KILL_BONUS;
            }

            // 2. Trapping: the enemy's Voronoi-owned area is less than their body length.
            if (enemyAreas[i] < enemy.length()) {
                bonus += ScoringConstants.OPPONENT_TRAP_BONUS;
            }
        }

        return bonus / Math.max(1, enemies.size());
    }

    /** Tail-chase rescue: only meaningful when **physical** room is
     *  genuinely tight (uses flood-fill count, not Voronoi). Enemy
     *  pressure shrinks Voronoi but doesn't shrink our corridor; we
     *  shouldn't switch into rescue mode just because an enemy reaches
     *  some distant cells faster. */
    public static double tailRescueBonus(boolean canReachTail, int tailDist,
                                          SurvivalArea.Area owned, int myLength) {
        boolean tight = owned.floodCount() < ScoringConstants.TAIL_BONUS_AREA_MULT * myLength;
        return (canReachTail && tight)
                ? ScoringConstants.TAIL_BONUS / (1.0 + tailDist)
                : 0.0;
    }

    /** Stretch / zig-zag: dual of tail-chase. In open space prefer
     *  candidates whose BFS path back to the tail is *long* — i.e. the
     *  body stays extended and the surrounding cells stay free. Caps at
     *  STRETCH_BONUS once tailDist ≥ myLength (fully extended).
     *
     *  Two gates, both must pass:
     *    - {@code !floodTight} — physical room exists (no point pulling
     *      apart in a corridor that already constrains the body).
     *    - {@code ownedCount >= myLength} — the room is **ours**. Stretching
     *      into a 100-cell region where the enemy wins the race to 96 of
     *      them is just walking into their Voronoi pocket; the previous
     *      gate missed this and let stretch=150 outweigh a clear
     *      survival-area drop. */
    public static double stretchBonus(boolean canReachTail, int tailDist,
                                       SurvivalArea.Area owned, int myLength) {
        boolean floodTight = owned.floodCount() < ScoringConstants.TAIL_BONUS_AREA_MULT * myLength;
        if (!canReachTail || floodTight) return 0.0;
        if (owned.rawCount() < myLength)  return 0.0;
        double frac = Math.min(tailDist, myLength) / (double) Math.max(myLength, 1);
        return ScoringConstants.STRETCH_BONUS * frac;
    }

    /** Centre preference — Chebyshev distance to the board centre. */
    public static double centreBonus(BoardGrid grid, Coord next) {
        int cx = (grid.getWidth()  - 1) / 2;
        int cy = (grid.getHeight() - 1) / 2;
        int centreDist = Math.max(Math.abs(next.x() - cx), Math.abs(next.y() - cy));
        return ScoringConstants.CENTER_BONUS / (1.0 + centreDist);
    }

    /** Hard penalty per wall the candidate cell physically touches.
     *  Discourages the "circle along the border" failure mode where a
     *  wall + our body leave only two free directions. Corners pay
     *  double (two walls). The soft {@link #centreBonus} gradient is
     *  too weak (~16 at edge vs 100 at centre) to break this on its
     *  own once stretch / survival plateau along an edge corridor. */
    public static double wallPenalty(BoardGrid grid, Coord next) {
        int walls = 0;
        if (next.x() == 0)                     walls++;
        if (next.x() == grid.getWidth()  - 1)  walls++;
        if (next.y() == 0)                     walls++;
        if (next.y() == grid.getHeight() - 1)  walls++;
        return ScoringConstants.WALL_PENALTY * walls;
    }

    /** Per-segment hazard drain forecast for the body state we'd commit
     *  to with this move. Counts hazard cells in the new body:
     *    new body = {next} ∪ oldBody[0..L−2]   (no eat — tail vacates)
     *             = {next} ∪ oldBody[0..L−1]   (eat — tail stays, length+1)
     *  Iff {@code next} is a food cell, the tail does NOT vacate, so
     *  any hazard segment at the tail keeps draining; otherwise the
     *  vacating tail saves us one segment of drain.
     *
     *  Differences between candidates this turn come from (a) head
     *  entering / leaving hazard and (b) eating-vs-not interaction
     *  with a hazard tail. Across turns it gives a faithful absolute
     *  cost — long body in sauce now scores proportionally to the
     *  HP we're actually losing. */
    public static double hazardDrainPenalty(BoardGrid grid, Coord next,
                                             List<Coord> myBody, Set<Coord> food) {
        boolean willEat = food.contains(next);
        int hazardSegs = grid.isHazard(next) ? 1 : 0;
        int upTo = willEat ? myBody.size() : myBody.size() - 1;
        for (int i = 0; i < upTo; i++) {
            if (grid.isHazard(myBody.get(i))) hazardSegs++;
        }
        return ScoringConstants.HAZARD_PENALTY * hazardSegs;
    }

    /** Food race — strictly closer, OR tied and we'd win the H2H.
     *  Suppressed when trapped, or when healthy and locked into a tight
     *  tail-loop (eating would grow us / disable tail-vacate and break
     *  the loop we're surviving on). */
    public static double foodBonus(int[][] myDist,
                                    Bfs.EnemyReach enemyReach,
                                    List<BattleSnake> enemies,
                                    Set<Coord> food,
                                    double foodWeight,
                                    boolean trapped,
                                    boolean canReachTail,
                                    SurvivalArea.Area owned,
                                    int myLength,
                                    int myHealth,
                                    BoardGrid grid) {
        if (food.isEmpty()) return 0.0;
        // Tight-loop check uses flood (physical room), not Voronoi.
        boolean tightLoop = canReachTail && owned.floodCount() < myLength * 1.5;
        if (trapped || (myHealth > 50 && tightLoop)) return 0.0;

        int[][] enemyDist  = enemyReach.dist();
        int[][] enemyOwner = enemyReach.owner();
        double best = 0.0;

        for (Coord f : food) {
            int md = Bfs.at(myDist, f);
            if (md >= Bfs.UNREACHABLE) continue;
            int ed = Bfs.at(enemyDist, f);

            int myArrival = md + 1;
            boolean win;
            if (myArrival < ed) {
                win = true;
            } else if (myArrival == ed) {
                int ownerIdx = enemyOwner[f.x()][f.y()];
                // Starvation override: if we are about to starve, a mutual death head-to-head
                // is better than certain death.
                boolean starving = myHealth < md + 5;
                win = (ownerIdx >= 0 && myLength > enemies.get(ownerIdx).length()) || starving;
            } else {
                win = false;
            }

            if (win) {
                double score = foodWeight / (1.0 + md);

                // Apply rules for risky food and enemy proximity
                boolean starving = myHealth < md + 10;
                if (!starving) {
                    // Rule 1: Risky food (corners or next to enemy)
                    boolean isCorner = isCorner(grid, f);
                    boolean nextToEnemy = isNextToEnemy(f, enemies);

                    if (isCorner || nextToEnemy) {
                        score -= ScoringConstants.RISKY_FOOD_PENALTY;
                    }

                    // Rule 2: Food close to enemy vs "safe" food
                    // If ed is small, the food is close to an enemy.
                    // If we have other "safer" food options, we might want to prioritize them.
                    // For now, let's penalize food that is "contested" (ed <= md + 2)
                    // compared to "guaranteed" food.
                    if (ed <= md + 2) {
                        score *= 0.5; // Halve the score for contested food
                    }
                }

                best = Math.max(best, score);
            }
        }
        return best;
    }

    private static boolean isCorner(BoardGrid grid, Coord c) {
        int walls = 0;
        if (c.x() == 0) walls++;
        if (c.x() == grid.getWidth() - 1) walls++;
        if (c.y() == 0) walls++;
        if (c.y() == grid.getHeight() - 1) walls++;
        return walls >= 2;
    }

    private static boolean isNextToEnemy(Coord f, List<BattleSnake> enemies) {
        for (BattleSnake enemy : enemies) {
            if (CoordUtils.manhattanDistance(f, enemy.head()) <= 1) {
                return true;
            }
        }
        return false;
    }
}

