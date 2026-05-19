package sneak.snaek.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModeStrategyFactoryTest {

    @Test
    public void testEveryModeProducesAnEngine() {
        for (GameMode mode : GameMode.values()) {
            ModularSnakeEngine engine = ModeStrategyFactory.engineFor(mode);
            assertNotNull(engine, "no engine for " + mode);
            assertNotNull(engine.config(), "no config for " + mode);
        }
    }

    @Test
    public void testDuelEnablesParanoidEnumeration() {
        EngineConfig cfg = ModeStrategyFactory.engineFor(GameMode.DUEL).config();
        assertEquals(3, cfg.enemyMovesPerCandidate);
        assertTrue(cfg.enableLookahead);
    }

    @Test
    public void testConstrictorBumpsLookaheadCandidates() {
        EngineConfig cfg = ModeStrategyFactory.engineFor(GameMode.CONSTRICTOR).config();
        assertTrue(cfg.lookaheadCandidates >= 6,
                "CONSTRICTOR should explore more candidates; got " + cfg.lookaheadCandidates);
    }

    @Test
    public void testStandardKeepsDefaultConfig() {
        EngineConfig cfg = ModeStrategyFactory.engineFor(GameMode.STANDARD).config();
        assertEquals(4, cfg.lookaheadCandidates);
        assertEquals(1, cfg.enemyMovesPerCandidate);
    }

    @Test
    public void testWrappedFallsBackToStandardBaseline() {
        ModularSnakeEngine wrapped = ModeStrategyFactory.engineFor(GameMode.WRAPPED);
        assertEquals(GameMode.WRAPPED, wrapped.mode());
        assertEquals(Personality.BULLY, wrapped.personality());
    }

    @Test
    public void testWrappedConstrictorFallsBackToConstrictor() {
        ModularSnakeEngine wc = ModeStrategyFactory.engineFor(GameMode.WRAPPED_CONSTRICTOR);
        assertEquals(GameMode.WRAPPED_CONSTRICTOR, wc.mode());
        assertEquals(Personality.TURTLE, wc.personality());
    }
}

