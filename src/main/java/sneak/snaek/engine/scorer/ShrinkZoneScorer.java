package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;
import sneak.snaek.engine.TurnContext;
import sneak.snaek.model.Coord;
import sneak.snaek.model.GameState;
import sneak.snaek.model.Ruleset;

/**
 * Royale-specific scorer that penalises cells which will become hazard
 * within the next {@link ScoringConstants#SHRINK_LOOKAHEAD_TURNS} turns.
 *
 * <p>Battlesnake's standard Royale shrink adds one hazard ring on a
 * randomly chosen side every {@code shrinkEveryNTurns} turns (default
 * 25). Because the side is random we can't know <em>which</em> edge
 * shrinks next — so we treat every non-hazard cell within
 * {@code projectedShrinks} of any edge as at risk. The penalty grows as
 * the projected shrink count catches up to the cell's distance from the
 * edge: cells already adjacent to a hazard frontier pay the full
 * {@link ScoringConstants#SHRINK_ZONE_PENALTY}; cells one ring deeper
 * pay it scaled by {@code 1 / projectedShrinks}; cells beyond the
 * projection horizon are unaffected.
 *
 * <p>Inactive (returns 0) outside Royale or when shrink metadata is missing.
 */
public class ShrinkZoneScorer implements Scorer {

    private final double weight;

    public ShrinkZoneScorer() { this(1.0); }
    public ShrinkZoneScorer(double weight) { this.weight = weight; }

    @Override
    public double score(MoveContext ctx) {
        TurnContext t = ctx.turn();
        GameState state = t.state();
        Integer shrinkEveryN = shrinkEveryNTurns(state);
        if (shrinkEveryN == null || shrinkEveryN <= 0) return 0.0;

        int lookahead = ScoringConstants.SHRINK_LOOKAHEAD_TURNS;
        int turnsUntilNextShrink = shrinkEveryN - (state.turn() % shrinkEveryN);
        if (turnsUntilNextShrink > lookahead) return 0.0;

        // How many shrinks could plausibly land within the lookahead window.
        // +1 because the next shrink itself counts.
        int projectedShrinks = 1 + (lookahead - turnsUntilNextShrink) / shrinkEveryN;

        Coord next = ctx.next();
        int w = t.grid().getWidth();
        int h = t.grid().getHeight();

        // Already in hazard? Caller already pays via HAZARD_PENALTY — don't double-charge.
        if (t.grid().isHazard(next)) return 0.0;

        int distFromEdge = Math.min(Math.min(next.x(), next.y()),
                                    Math.min(w - 1 - next.x(), h - 1 - next.y()));
        if (distFromEdge >= projectedShrinks) return 0.0;

        // Closer to edge ⇒ shrinks reach us sooner ⇒ heavier penalty.
        // ringsAway in [0..projectedShrinks-1]; 0 = immediate next shrink.
        int ringsAway = distFromEdge;
        double severity = (projectedShrinks - ringsAway) / (double) projectedShrinks;
        return -weight * ScoringConstants.SHRINK_ZONE_PENALTY * severity;
    }

    private static Integer shrinkEveryNTurns(GameState state) {
        if (state.game() == null) return null;
        Ruleset r = state.game().ruleset();
        if (r == null || r.settings() == null || r.settings().royalty() == null) return null;
        int n = r.settings().royalty().shrinkEveryNTurns();
        return n > 0 ? n : null;
    }
}
