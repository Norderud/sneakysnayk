package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for the "Gatekeeper" persona — intercepting enemies on their way to food.
 */
public class GatekeeperScorer implements Scorer {
    @Override
    public double score(MoveContext ctx) {
        return MoveScorer.gatekeeperBonus(ctx.turn(), ctx);
    }
    
    @Override
    public String name() {
        return "gatekeeper";
    }
}
