package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer that assigns a score to a candidate move.
 */
public interface Scorer {
    /**
     * Returns a score for the given move. Higher is better.
     */
    double score(MoveContext ctx);
    
    /**
     * Optional: name of the scorer for logging purposes.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
