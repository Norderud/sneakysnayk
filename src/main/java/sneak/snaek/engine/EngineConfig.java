package sneak.snaek.engine;

public class EngineConfig {
    /** How many candidate moves to explore with lookahead. */
    public static int lookaheadCandidates = 4;
    
    /** Target ratio of the timeout to use (0.7 means use up to 70% of timeout). */
    public static double timeBudgetRatio = 0.7;
    
    /** Enable/Disable lookahead. */
    public static boolean enableLookahead = true;
}
