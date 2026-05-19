package sneak.snaek.engine;

/**
 * Per-engine tuning knobs for the decision pipeline.
 *
 * <p>These were previously {@code public static} fields shared JVM-wide.
 * Now that {@link ModularSnakeEngine} instances are built per game by
 * {@link ModeStrategyFactory}, each mode gets its own config so e.g. a
 * Constrictor game can run deeper lookahead than a Standard game in the
 * same JVM without trampling shared state.
 */
public class EngineConfig {

    /** How many top 1-ply candidate moves to explore with lookahead. */
    public int lookaheadCandidates = 4;

    /** Target ratio of the per-move timeout to use (0.7 = up to 70%). */
    public double timeBudgetRatio = 0.7;

    /** Enable/disable lookahead entirely. */
    public boolean enableLookahead = true;

    /**
     * How many candidate moves to enumerate <em>per enemy</em> inside
     * {@link ModularSnakeEngine#evaluateLookahead}. Default 1 keeps the
     * legacy behavior (single best-guess enemy reply, expectimax). DUEL
     * sets this to 3 to enumerate every plausible enemy move and take
     * the minimum re-score across them (true paranoid 2-ply, affordable
     * with only one opponent).
     */
    public int enemyMovesPerCandidate = 1;

    /**
     * Use time-aware flood fill ({@link sneak.snaek.strategy.TimedBfs}) in
     * preference to plain {@link sneak.snaek.strategy.Bfs}. When true,
     * BFS respects {@link sneak.snaek.board.VacateMap} — snake bodies
     * only block cells until they slither out of the way. Default true;
     * flip to false for A/B comparison against the legacy static-wall
     * BFS.
     */
    public boolean timeAwareFlood = true;

    public EngineConfig() {}

    /** Convenience for fluent overrides in mode factories. */
    public EngineConfig with(java.util.function.Consumer<EngineConfig> mutator) {
        mutator.accept(this);
        return this;
    }
}
