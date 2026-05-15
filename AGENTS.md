# AGENTS.md – super-sneyk BattleSnake
## Project Overview
Java 21 BattleSnake bot. No framework – uses raw `com.sun.net.httpserver.HttpServer` on port 8080. JSON via Gson. Build tool: Maven.
Decision logic is intentionally simple: **one-ply greedy** scoring (no minimax, no search tree). Latency target is well under the 500 ms BattleSnake budget.
## Build & Run
```bash
mvn package                  # produces target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar
# Also produces target/battlesnake.zip (deployment archive via zip.xml assembly)
```
> ⚠️ `pom.xml` `<mainClass>` references a stale package (`com.mastercard.e117387.snaeksneak`). The real entry point is `sneak.snaek.FierceBattleSnakeApplication`.
> ⚠️ `pom.xml` uses `<release>${java.version}</release>` which resolves to the *runtime* version (e.g. `21.0.10`) and breaks `maven-compiler-plugin`. Fix to `<release>21</release>` or rely on `maven.compiler.source/target` only.
**Multi-instance launch** — `main()` accepts optional `[port] [name] [color]` args so two JVMs can run side-by-side:
```bash
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar 8080 "Snake-A" "#ff9900"
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar 8081 "Snake-B" "#00aaff"
```
**Deployment tunnels** (noted in source):
```bash
ssh -N -o ServerAliveInterval=60 -R 8091:localhost:8080 -p 2222 battlesnake@devign-snake.dev.mastercard.int
ssh -N -o ServerAliveInterval=60 -R 8092:localhost:8081 -p 2222 battlesnake@devign-snake.dev.mastercard.int
```
**Dependencies**: Gson (JSON), SLF4J + Logback (logging via `org.slf4j.Logger` / `getLogger`).

**Logs**:
- Application log → console (via `STDOUT` appender in `src/main/resources/logback.xml`).
- Battle outcomes → `logs/scores.log` only (dedicated `score` logger, `additivity=false`). One line per game, written from `FierceBattleSnakeApplication.endSignal`. Fields: `result` (WIN/LOSS/DRAW), `gameId`, `mode` (SOLO/DUEL/FFA inferred from initial snake count), `turns`, `us`, `usLength`, `opponents` (captured at `/start`), `survivors` (from final state).
## Package Structure
| Package | Role |
|---|---|
| `sneak.snaek` | HTTP server (`FierceBattleSnakeApplication`) + decision orchestrator (`SnakeEngine`, containing the nested `PathOptions` safety filter) |
| `sneak.snaek.model` | Immutable Java records — API JSON deserialization (`GameState`, `BattleSnake`, `Board`, `Coord`, `Move`, …) |
| `sneak.snaek.board` | Board representation (`BoardGrid`) and grid utilities (`CoordUtils`) |
| `sneak.snaek.strategy` | Move scoring: `MoveScorer` (entry point), `FloodFill` (reachable area) |
## Move Pipeline (critical dataflow)
```
POST /move → FierceBattleSnakeApplication.move()
  → SnakeEngine.move(GameState)
      1. PathOptions.avoidBlocked()            // remove OOB/body collisions
         PathOptions.avoidHeadToHeadLosses()   // remove squares reachable by equal/longer enemies
      2. If 1 safe move → return immediately
      3. For each remaining move: MoveScorer.score(grid, head, move, enemies, food)
         Pick the move with the highest score.
```
## Scoring (`MoveScorer.score`)
For a candidate `move`, with `next = neighbor(head, move)`:
```
score = survival                                    // see below
      + FOOD_BONUS / (1 + myDist)  if  ∃ f ∈ food   // dominant food-race bonus
                                       myDist(next → f) < enemyDist(any enemy head → f)
      + TAIL_BONUS / (1 + manhattan(next, myTail))  // tail-chase tiebreaker

where survival = area − TRAP_PENALTY                if area < myLength
              = min(area, AREA_CAP_MULT * myLength) otherwise
```
- `FOOD_BONUS = 10_000` — large enough that **any safe food we win** beats survival ties. This is deliberate: the previous design gated food on health and never chased it aggressively.
- `TRAP_PENALTY = 1_000` — applied when the reachable pocket is smaller than our body length (we'd die before our tail vacates). Heavy enough to lose to any roomier alternative, but finite so the bot can still pick a trapped cell when it's the *only* option.
- `TAIL_BONUS = 100`, `AREA_CAP_MULT = 2` — together prevent the long-snake "wander into a U-shape and seal it" failure mode in solo / open-board play. The area cap means once a move offers ≥ 2× our length of room, *all* such moves count as equally safe; the tail-chase bonus then steers us along our own body so we keep looping in a guaranteed-safe corridor. In tight spaces (`area < length`) the trap penalty dominates, so this never overrides survival.
- **Lead dampener** (`LEAD_THRESHOLD = 4`): food bonus weight is `FOOD_BONUS / (1 + max(0, lead − LEAD_THRESHOLD))` where `lead = myLength − maxEnemyLength`. Smooth arithmetic decay — at lead ≤ 4 the bonus is full (10 000); at lead 5 it halves, lead 6 thirds, … so the bot keeps grabbing close food well into a comfortable lead and only relaxes once *very* far ahead. Disabled in solo (no enemies) — full chase always.
- **Trapped → no food bonus.** When `area < myLength` the food term is skipped entirely. Otherwise the food race (up to `FOOD_BONUS=10_000`) would dominate the (intentionally small) trap penalty and the bot would happily eat itself into a closed pocket.
- "Strictly closer" check uses two BFS passes on `BoardGrid`: one from the candidate cell, one multi-source from all enemy heads. Bodies block; tail-vacate rule applies via `BoardGrid` construction. The enemy BFS also tracks the *closest enemy index* per cell so we can resolve ties: if `myDist == enemyDist` and we are **strictly longer** than the contesting enemy (we'd win the H2H), the food still counts. Equal-length ties are intentionally NOT contested — both snakes die.
- The food bonus considers the *closest winnable* food (max over `1/(1+md)`).
## Key Conventions
- **One-ply only.** Do not reintroduce minimax / Voronoi / multi-component heuristics without measuring latency — the project was simplified specifically because the prior design hit ~500 ms.
- **Tail-freeing rule**: the last body segment is excluded from the blocked set when `body[last-1] != body[last]` (snake didn't eat last turn). Implemented in `BoardGrid.markSnakeBody()`.
- **`PathOptions.avoidHeadToHeadLosses`** only filters enemies that are *equal or longer* — shorter enemies can be safely contested. If every move is dangerous it keeps the body-safe set (no forced suicide).
- **Coord system**: `x` = column (left→right), `y` = row (bottom→top). `CoordUtils.neighbor(head, Move)` is the canonical way to compute adjacent cells.
- **Mode handling removed.** There is no `Mode.DUEL` / `Mode.FFA` split anymore — same logic in all games.
- No test suite — validate logic changes manually against the BattleSnake simulator.
## Adding/Tuning Behaviour
1. New score component: add a term inside `MoveScorer.score()`. Normalise it relative to `floodFill` area (max ≈ board cells = 121) so it doesn't accidentally dominate the food bonus.
2. To make food-chasing more/less aggressive, tune `FOOD_BONUS` or relax the "strictly closer" condition to `<=` in `MoveScorer`.
3. Spatial helpers (BFS, flood-fill) live in `sneak.snaek.strategy`; grid/coord types in `sneak.snaek.board`.