package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for positional heuristics (H2H, Centre, Wall, Hazard).
 */
public class PositionScorer implements Scorer {
    @Override
    public double score(MoveContext ctx) {
        if (ctx.trapped()) return 0.0;
        
        double centre = MoveScorer.centreBonus(ctx.turn().grid(), ctx.next());
        double wall = MoveScorer.wallPenalty(ctx.turn().grid(), ctx.next());
        double hazard = MoveScorer.hazardDrainPenalty(ctx.turn().grid(), ctx.next(), ctx.turn().state().you().body(), ctx.turn().food());
        
        return centre - wall - hazard;
    }
}
