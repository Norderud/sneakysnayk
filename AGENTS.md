# AGENTS.md – super-sneyk BattleSnake

## Project Overview

Java 21 BattleSnake bot. No web framework — raw `com.sun.net.httpserver.HttpServer` on port 8080. JSON via Gson. Build tool: Maven.

Decision logic is a **modular pipeline of filters + scorers**, with optional **2-ply lookahead** via a forward `Simulator`. Multiple **personalities** (BULLY, MIDAS, TURTLE, PARASITE, DUELIST) compose different scorer weightings on top of the same engine. A **`GameMode` layer** sits above personalities and auto-builds a tuned engine per game (STANDARD / DUEL / ROYALE / CONSTRICTOR / SOLO / WRAPPED*). Latency target is under the 500 ms BattleSnake budget; lookahead is gated by a time budget (`EngineConfig.timeBudgetRatio`, default 0.7).

> ⚠️ **Do NOT trust prior versions of this file.** The codebase has evolved significantly — Voronoi area control, asymmetric H2H attack bonuses, opponent-trap scoring, anti-coiling tail bonus, hazard/wall/center heuristics, and 2-ply lookahead are all present today.

## Build & Run

```bash
mvn package                  # produces target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar
# Also produces target/battlesnake.zip (deployment archive via zip.xml assembly)
```


**Multi-instance launch** — `main()` accepts optional `[port] [name] [color]` args. Personality is no longer a CLI flag — `GameMode` auto-selects per game.

```bash
java -jar target/...-jar-with-dependencies.jar 8080 "Snake-A" "#ff9900"
java -jar target/...-jar-with-dependencies.jar 8081 "Snake-B" "#00aaff"
```

**Deployment tunnels** (noted in source):

```bash
ssh -N -o ServerAliveInterval=60 -R 8091:localhost:8080 -p 2222 battlesnake@devign-snake.dev.mastercard.int
ssh -N -o ServerAliveInterval=60 -R 8092:localhost:8081 -p 2222 battlesnake@devign-snake.dev.mastercard.int
```

**Local benchmarking** — `scripts/benchmark.ps1` and `scripts/run-local-cli.ps1` run games against the official Battlesnake rules engine. They look for `battlesnake.exe` in this order: `C:\Users\e117387\workspaces\random\rules\battlesnake.exe`, the project root, then `PATH`. `benchmark.ps1` supports a `-GameType` parameter (e.g. `standard`, `solo`, `wrapped`).

**Dependencies**: Gson (JSON), SLF4J + Logback (logging via `org.slf4j.Logger` / `getLogger`).

**Logs**:
- Application log → console (via `STDOUT` appender in `src/main/resources/logback.xml`).
- Battle outcomes → `logs/scores.log` (dedicated `score` logger, `additivity=false`). One line per game from `FierceBattleSnakeApplication.endSignal`. Fields: `result` (WIN/LOSS/DRAW), `gameId`, `mode` (detected `GameMode`), `turns`, `us`, `usLength`, `opponents`, `survivors`.
- Move decisions → `logs/decisions.log` (dedicated `decision` logger, `additivity=false`). One line per turn from `ModularSnakeEngine.move` — prefixed with `mode=<GameMode>`, all candidate moves, per-scorer breakdown, or the filter that pruned them.

## Package Structure

| Package | Role |
|---|---|
| `sneak.snaek` | HTTP server (`FierceBattleSnakeApplication`) — per-game engine cache keyed by `gameId` |
| `sneak.snaek.engine` | Decision orchestrator: `ModularSnakeEngine`, `TurnContext`, `MoveContext`, `EngineConfig`, `Personality`, `PersonalityEngineFactory`, `GameMode`, `ModeDetector`, `ModeStrategyFactory` |
| `sneak.snaek.engine.filter` | `MoveFilter`, `CollisionFilter`, `HeadToHeadFilter` |
| `sneak.snaek.engine.scorer` | `Scorer`, `MoveScorer` (shared helpers), `ScoringConstants`, plus `Survival/Food/Aggression/Tail/Position/Duelist/Parasite/ShrinkZone/Compactness` scorers |
| `sneak.snaek.model` | Immutable Java records — API JSON (`GameState`, `BattleSnake`, `Board`, `Coord`, `Move`, `Ruleset`, `RuleSettings`, …) |
| `sneak.snaek.strategy` | BFS (`Bfs`, `TimedBfs`), Voronoi area (`SurvivalArea`), plus `Dfs` / `AStar` helpers |
| `sneak.snaek.board` | `BoardGrid` (blocked/hazard/food grid with tail-vacate), `VacateMap` (per-cell vacate-turn schedule), `CoordUtils` |
| `sneak.snaek.sim` | `Simulator.step()` — forward state projection for lookahead |

## Move Pipeline

```
POST /move → FierceBattleSnakeApplication.move()
  → ModularSnakeEngine.move(GameState)
      1. TurnContext.from(state, personality)
           // grid, enemy list, multi-source enemy BFS (dist + owner + lengths),
           // food weights, startTimeNanos
      2. Apply MoveFilters (in order):
           - CollisionFilter   // OOB + body collisions
           - HeadToHeadFilter  // squares reachable by equal/longer enemies
                               //   (DUELIST: strictly longer only)
                               // keeps body-safe set if pruning would zero it out
      3. If 0 moves → return UP. If 1 move → return it.
      4. 1-ply scoring: for each candidate move, build MoveContext (per-cell
         BFS from the candidate, SurvivalArea Voronoi result) and sum all
         enabled Scorers.
      5. If EngineConfig.enableLookahead && time budget allows:
           a. Take top N (default 4) candidates by 1-ply score.
           b. For each, predict one enemy reply per opponent
              (predictEnemyMove: center pull + asymmetric H2H avoidance).
           c. Simulator.step(joint move) → new GameState.
           d. Re-score from new state. Combined = (s1 + 0.9*s2) / 1.9.
      6. Pick highest-scoring move; log full breakdown to logs/decisions.log.
```

## Scoring

Each `Scorer` returns a double; the engine sums them. All magnitudes live in `ScoringConstants` with rationale comments — tune there, not in scorers. Shared math (food race, aggression, BFS lookups) is in static helpers on `MoveScorer`.

### Active scorers (default BULLY personality)

| Scorer | What it rewards / penalizes | Key constants |
|---|---|---|
| **SurvivalScorer** | Reachable area weighted by Voronoi ownership and hazard cost; heavy penalty when the pocket is smaller than our body (uses both `floodCount` for physical room and `rawCount` for enemy pressure). | `SURVIVAL_WEIGHT=3.0`, `TRAP_PENALTY=2000`, `HAZARD_AREA_WEIGHT` |
| **FoodScorer** | Closest food we win the race to, with risky-food penalty (corners, enemy-adjacent food), contested-food halving (`enemyDist ≤ myDist+2`), lead dampener, and starvation override that permits mutual-death H2H below `HEALTH_URGENCY_THRESHOLD`. | `FOOD_BONUS=750`, `RISKY_FOOD_PENALTY=300`, `LEAD_THRESHOLD=4`, `HEALTH_URGENCY_THRESHOLD=60` |
| **AggressionScorer** | H2H kill bonus when the candidate cell is reachable by a strictly shorter enemy head; opponent-trap bonus that grows as we reduce a single enemy's Voronoi area; per-enemy averaging. | `H2H_KILL_BONUS=200`, `OPPONENT_TRAP_BONUS=600`, `BULLY_SCORE_FACTOR=50` |
| **TailScorer** | Tail-rescue bonus (chase own tail when boxed in) AND stretch bonus (lengthen `head→tail` Manhattan distance in open space) — anti-coiling in both directions. | `TAIL_BONUS=100`, `STRETCH_BONUS=100` |
| **PositionScorer** | Center pull, wall/corner penalty, multi-segment hazard penalty (counts every body segment currently in hazard with HP buffer awareness). | `CENTER_BONUS=100`, `WALL_PENALTY=50`, `HAZARD_PENALTY=250`, `HAZARD_HP_BUFFER=20` |

### Personality scorers (composed in `PersonalityEngineFactory`)

- **DuelistScorer** (DUELIST only) — seeks H2H combat; pairs with relaxed `HeadToHeadFilter` (accepts equal-length contests) and `myArrivalOffset=0` in Voronoi so we treat ourselves as equal-speed to enemies. Bonus: `DUELIST_BONUS=300`.
- **ParasiteScorer** (PARASITE only) — rewards shadowing larger snakes at a 1–2 cell offset. Bonus: `PARASITE_BONUS=150`.

### Mode scorers (composed in `ModeStrategyFactory`)

- **ShrinkZoneScorer** (ROYALE) — projects the next `SHRINK_LOOKAHEAD_TURNS=8` hazard rings from `Ruleset.settings().royalty().shrinkEveryNTurns()` and penalises edge cells about to become hazard. Skips already-hazard cells (`HAZARD_PENALTY` covers those). Penalty: `SHRINK_ZONE_PENALTY=150`, scaled by imminence.
- **CompactnessScorer** (CONSTRICTOR) — penalises coiled shapes (`head ↔ tail ≤ 1` is near-fatal when you grow every turn) and rewards stretched bodies (`head→tail ≥ length × COMPACTNESS_STRETCH_FRACTION`). Constants: `COMPACTNESS_PENALTY=1000`, `COMPACTNESS_STRETCH_BONUS=200`, `COMPACTNESS_STRETCH_FRACTION=0.6`. Inactive for length < 4.

### Personalities

| Personality | Tuning |
|---|---|
| `BULLY` (default) | Equal weights — full Aggression + Survival. |
| `MIDAS` | Food ×1.5, Aggression ×0.2 — food-focused, low aggression. |
| `TURTLE` | Survival ×2.0, Food ×0.5, Aggression ×0.0, Tail ×1.5 — extreme defense. |
| `PARASITE` | Aggression ×0.5 + ParasiteScorer — shadow larger snakes. |
| `DUELIST` | DuelistScorer + relaxed H2H filter + symmetric Voronoi offset. |

### Game modes (composed in `ModeStrategyFactory`)

`GameMode` is auto-detected at `/start` by `ModeDetector.detect(state)` from `Ruleset.name` + initial snake count. Each game gets its own `ModularSnakeEngine` cached by `gameId` in `FierceBattleSnakeApplication`; entries are removed at `/end`. If `/move` arrives for an unknown `gameId` (e.g. server restart) a fresh engine is built on demand.

| Mode | Base personality | Mode-specific tuning |
|---|---|---|
| `STANDARD` (≥3 snakes, standard ruleset) | BULLY | defaults |
| `DUEL` (2 snakes, standard ruleset) | DUELIST | `EngineConfig.enemyMovesPerCandidate = 3` → paranoid enumeration over all enemy replies (single opponent, well within budget). Aggression ×1.5. |
| `ROYALE` | BULLY | `ShrinkZoneScorer(weight=2.0)`, Aggression ×0.5 (opponents often die to hazard on their own). |
| `CONSTRICTOR` | TURTLE | **FoodScorer removed** (no food spawns), Survival ×2.0, `CompactnessScorer`, `lookaheadCandidates = 6` (deterministic, deeper search affordable). |
| `SOLO` | MIDAS | defaults — already implicit (no enemies). |
| `WRAPPED` | STANDARD fallback | Detected + logged only; toroidal BFS not implemented. `engine.setMode(WRAPPED)` keeps the tag for logs. |
| `WRAPPED_CONSTRICTOR` | CONSTRICTOR fallback | As above. |
| `UNKNOWN` | STANDARD fallback | Warn-log + STANDARD engine. |

`EngineConfig` is now per-instance (no longer static). The `enemyMovesPerCandidate` knob controls paranoid lookahead: `1` = legacy expectimax (best-guess enemy reply), `k > 1` = enumerate top-k enemy moves per opponent and take the worst joint-move score (paranoid min). DUEL sets `k=3`.

## Key Conventions

- **Coord system**: `x` = column (left→right), `y` = row (bottom→top). `CoordUtils.neighbor(head, Move)` is the canonical way to compute adjacent cells.
- **Tail-vacate rule**: `BoardGrid.markSnakeBody` excludes the last body segment from blocked iff `body[last-1] != body[last]` (snake didn't eat last turn). Both BFS and the simulator respect this.
- **HeadToHeadFilter** prunes only against equal-or-longer enemies (strictly longer for DUELIST) and refuses to zero out the candidate set — no forced suicide.
- **Voronoi ownership** (`SurvivalArea.compute`): a cell is ours iff we arrive strictly first, OR we tie and we are strictly longer. Hazards count `HAZARD_AREA_WEIGHT` instead of 1.0. The `myArrivalOffset` parameter lets DUELIST treat itself as equal-speed (default 1 = we move next turn, enemies are already at their heads).
- **Time-aware flood fill** (`TimedBfs` + `VacateMap`, default on via `EngineConfig.timeAwareFlood=true`): the BFS knows that each body segment at index `i` of a length-`L` snake vacates at absolute turn `(L − i) + (justAte ? 1 : 0)`. So a cell currently behind our own neck is reachable if our path is long enough that the body has slithered past by the time we arrive. Plain `Bfs` (static-wall model) is kept as the fallback path when the flag is off — useful for A/B regression. Per-candidate `tflood=` is logged next to `flood=` in `logs/decisions.log` for diffing. Known minor approximation: snakes cannot wait in place, so cells reachable only by "go straight then wait for vacate" are conservatively marked unreachable.
- **Lookahead is expectimax by default** (`enemyMovesPerCandidate=1`, single best-guess enemy reply). DUEL upgrades to **paranoid min** by setting `enemyMovesPerCandidate=3` and taking the worst joint-move re-score. Iterative deepening + transposition table still on the roadmap.
- **Mode-specialized strategies**: STANDARD / DUEL / ROYALE / CONSTRICTOR / SOLO are tactically tuned via `ModeStrategyFactory`. WRAPPED / WRAPPED_CONSTRICTOR are detected and tagged but fall back to non-wrapped engines — toroidal BFS is a known TODO (`BoardGrid.inBounds`, `Bfs.computeEnemyReach`, `CoordUtils.neighbor`).
- **Time budget**: `TurnContext.startTimeNanos` + `elapsedMillis()` is checked before each lookahead candidate; lookahead is skipped past `timeBudgetRatio * 500 ms`.
- **Unit tests** — `mvn test`.

## Adding / Tuning Behaviour

1. **New Filter**: implement `MoveFilter` and add via `engine.addFilter(...)` in `PersonalityEngineFactory` (or a custom factory).
2. **New Scorer**: implement `Scorer.score(MoveContext)` — use the precomputed BFS / `SurvivalArea` on `MoveContext`; reuse helpers on `MoveScorer` rather than re-BFSing. Add via `engine.addScorer(...)`.
3. **New Personality**: add to `Personality` enum, then a `case` in `PersonalityEngineFactory.create()`.
4. **New GameMode**: add to `GameMode` enum, extend `ModeDetector.detect()` and add a `build<Mode>()` branch in `ModeStrategyFactory.engineFor()`.
5. **Tuning weights**: edit `ScoringConstants` — all magnitudes are documented relative to each other there.
6. **Lookahead tuning**: `EngineConfig.lookaheadCandidates`, `enableLookahead`, `timeBudgetRatio`, `enemyMovesPerCandidate`. The combine formula `(s1 + 0.9*s2) / 1.9` lives in `ModularSnakeEngine`.
7. **Custom Engine**: `new ModularSnakeEngine()` and compose your own filter/scorer set — `PersonalityEngineFactory` and `ModeStrategyFactory` are the examples.

## Git & Commits

- Include `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>` trailer when authored with the Copilot CLI.
- Do NOT include any `Co-authored-by: Junie ...` trailer unless explicitly requested.
