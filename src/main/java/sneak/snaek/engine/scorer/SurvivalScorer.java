package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for survival based on reachable area (Voronoi-weighted).
 */
public class SurvivalScorer implements Scorer {
    @Override
    public double score(MoveContext ctx) {
        return ctx.trapped()
                ? ctx.owned().weighted() - ScoringConstants.TRAP_PENALTY
                : ctx.owned().weighted() * ScoringConstants.SURVIVAL_WEIGHT;
    }
}
