package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for survival based on reachable area (Voronoi-weighted).
 */
public class SurvivalScorer implements Scorer {
    private final double weight;

    public SurvivalScorer() {
        this(1.0);
    }

    public SurvivalScorer(double weight) {
        this.weight = weight;
    }

    @Override
    public double score(MoveContext ctx) {
        double base = ctx.trapped()
                ? ctx.owned().weighted() - ScoringConstants.TRAP_PENALTY
                : ctx.owned().weighted() * ScoringConstants.SURVIVAL_WEIGHT;
        return base * weight;
    }
}
