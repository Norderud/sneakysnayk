package sneak.snaek.engine;

import sneak.snaek.engine.filter.CollisionFilter;
import sneak.snaek.engine.filter.HeadToHeadFilter;
import sneak.snaek.engine.scorer.*;

public class PersonalityEngineFactory {

    public static ModularSnakeEngine create(Personality personality) {
        ModularSnakeEngine engine = new ModularSnakeEngine()
                .setPersonality(personality)
                .addFilter(new CollisionFilter())
                .addFilter(new HeadToHeadFilter());

        switch (personality) {
            case MIDAS:
                // Focused on food and growth, less aggressive
                engine.addScorer(new SurvivalScorer())
                      .addScorer(new FoodScorer(1.5)) // 50% more focus on food
                      .addScorer(new AggressionScorer(0.2)) // 80% less focus on aggression
                      .addScorer(new TailScorer())
                      .addScorer(new PositionScorer());
                break;
            case TURTLE:
                // Extremely defensive, avoids everyone
                engine.addScorer(new SurvivalScorer(2.0)) // Double survival weight
                      .addScorer(new FoodScorer(0.5)) // Low food priority
                      .addScorer(new AggressionScorer(0.0)) // No aggression
                      .addScorer(new TailScorer(1.5)) // High tail rescue priority
                      .addScorer(new PositionScorer());
                break;
            case PARASITE:
                // Shadow larger snakes, less aggressive on H2H
                engine.addScorer(new SurvivalScorer())
                      .addScorer(new FoodScorer())
                      .addScorer(new AggressionScorer(0.5)) // Half aggression
                      .addScorer(new ParasiteScorer())
                      .addScorer(new TailScorer())
                      .addScorer(new PositionScorer());
                break;
            case DUELIST:
                // Actively seek head-to-head combat, combine with high aggression
                engine.addScorer(new SurvivalScorer())
                      .addScorer(new FoodScorer(0.8)) // Less focus on food
                      .addScorer(new AggressionScorer(1.5)) // Higher focus on trapping/H2H kills
                      .addScorer(new DuelistScorer(1.0))
                      .addScorer(new TailScorer())
                      .addScorer(new PositionScorer());
                break;
            case BULLY:
            default:
                // The "Territorial Bully" - existing behavior
                engine.addScorer(new SurvivalScorer())
                      .addScorer(new FoodScorer())
                      .addScorer(new AggressionScorer())
                      .addScorer(new TailScorer())
                      .addScorer(new PositionScorer());
                break;
        }
        return engine;
    }
}
