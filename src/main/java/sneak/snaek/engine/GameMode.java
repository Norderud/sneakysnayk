package sneak.snaek.engine;

/**
 * High-level BattleSnake game mode, detected per game at {@code /start}.
 *
 * <p>{@link ModeDetector} maps the raw {@code Ruleset.name} + initial
 * snake count to one of these. {@link ModeStrategyFactory} then builds
 * the {@link ModularSnakeEngine} (filters + scorers + {@link EngineConfig})
 * tuned for that mode.
 */
public enum GameMode {
    /** Standard ruleset with 3+ snakes. Default BULLY behavior. */
    STANDARD,

    /**
     * Standard ruleset with exactly 2 snakes. Battlesnake has no "duel"
     * ruleset name — this is inferred from snake count, but it's
     * tactically different enough to deserve its own engine (length
     * race, paranoid lookahead is affordable, contested H2H is fine).
     */
    DUEL,

    /** Solo / 1-snake. Pure survival + food. */
    SOLO,

    /** Royale — board shrinks via expanding hazard. */
    ROYALE,

    /** Constrictor — no food, every move grows. */
    CONSTRICTOR,

    /** Wrapped — edges teleport. Currently detected only; falls back to STANDARD. */
    WRAPPED,

    /** Wrapped + Constrictor combined variant. Currently falls back to CONSTRICTOR. */
    WRAPPED_CONSTRICTOR,

    /** Anything we couldn't classify. Falls back to STANDARD. */
    UNKNOWN
}
