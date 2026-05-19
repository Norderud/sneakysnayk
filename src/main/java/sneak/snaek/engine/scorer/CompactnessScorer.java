package sneak.snaek.engine.scorer;

import sneak.snaek.board.CoordUtils;
import sneak.snaek.engine.MoveContext;
import sneak.snaek.model.Coord;

/**
 * Constrictor-mode scorer that rewards staying physically extended and
 * penalises coiled (head ≈ tail) shapes.
 *
 * <p>In Constrictor every move grows the snake by 1 (no food spawns,
 * tail never recedes). A snake whose head is adjacent to its own tail
 * cannot escape its own body next turn and dies. The existing
 * {@link TailScorer} {@code stretchBonus} (≤ 100 pts) is far too small
 * to dominate when food and aggression terms run into the 500–2000
 * range — and in Constrictor those terms still influence the score even
 * though they're tactically less meaningful. This scorer adds a much
 * stronger, asymmetric signal sized to override anything but a frank
 * trap.
 *
 * <p>Output for a candidate cell {@code next}:
 * <ul>
 *   <li>{@code manhattan(next, tail) ≤ 1} ⇒ {@code -COMPACTNESS_PENALTY}
 *       (we'll collide with our own tail next move).</li>
 *   <li>{@code manhattan(next, tail) ≥ length * COMPACTNESS_STRETCH_FRACTION}
 *       ⇒ {@code +COMPACTNESS_STRETCH_BONUS} (body well-extended).</li>
 *   <li>linear interpolation between.</li>
 * </ul>
 *
 * <p>Inactive (returns 0) when {@code length < 4} — too short for the
 * notion to apply meaningfully.
 */
public class CompactnessScorer implements Scorer {

    private final double weight;

    public CompactnessScorer() { this(1.0); }
    public CompactnessScorer(double weight) { this.weight = weight; }

    @Override
    public double score(MoveContext ctx) {
        int length = ctx.turn().state().you().length();
        if (length < 4) return 0.0;

        Coord next = ctx.next();
        Coord tail = ctx.turn().myTail();
        int d = CoordUtils.manhattanDistance(next, tail);

        if (d <= 1) {
            return -weight * ScoringConstants.COMPACTNESS_PENALTY;
        }

        double stretchThreshold = length * ScoringConstants.COMPACTNESS_STRETCH_FRACTION;
        if (d >= stretchThreshold) {
            return weight * ScoringConstants.COMPACTNESS_STRETCH_BONUS;
        }

        // Linear interpolation between -PENALTY (at d=1) and +BONUS (at d=stretchThreshold).
        double t = (d - 1) / (stretchThreshold - 1);
        double penaltyEnd = -ScoringConstants.COMPACTNESS_PENALTY;
        double bonusEnd   =  ScoringConstants.COMPACTNESS_STRETCH_BONUS;
        return weight * (penaltyEnd + t * (bonusEnd - penaltyEnd));
    }
}
