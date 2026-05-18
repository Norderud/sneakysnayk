# super-sneyk — Improvement Plan

Ordered by **(benefit − complexity)**. Items 1 & 2 below the line are
already implemented (kept here for context). Tackle from the top.

---

## ✅ Done

- **The Parasite personality** — Added shadowing behavior where the bot follows enemy tails.
- **Eliminate `Coord` allocation in BFS inner loop** — Inlined `(±1, 0), (0, ±1)`
  deltas and added integer-based `isBlocked`/`isHazard` checks in `BoardGrid`.
  Significant reduction in short-lived objects during per-turn BFS.
- **Cache enemy lengths into an `int[]`** — `Bfs.computeEnemyReach` now
  builds a primitive array of lengths, used by `SurvivalArea`, `MoveScorer`,
  and `HeadToHeadFilter` to avoid repeated method calls and list overhead.
- **Hoist enemy BFS out of the per-move loop** — `MoveScorer.computeEnemyReach`
  is called once per turn in `SnakeEngine.move()` and reused for every
  candidate. Cuts per-turn BFS by ~33%.
- **BFS-based tail distance** — `MoveScorer` now uses path distance to the
  tail (via the `myDist` array already computed for the candidate cell)
  instead of Manhattan, with a fallback to the tail's nearest reachable
  neighbour when the tail cell itself is still blocked (post-eat).
- **Hazard awareness** — `ScoringConstants` and `MoveScorer` now account for
  hazard drain per body segment and inflate HP urgency when in sauce.
- **Win-the-H2H bonus against shorter enemies** — Presses smaller snakes
  by seeking head-to-head positions where we have the length advantage.
- **Boolean-array flood fill** — `SurvivalArea` now uses the pre-allocated
  BFS distance grids for area calculation, eliminating the old `FloodFill`
  class and its `HashSet` allocations.
- **Pathfinding Suite (A*, DFS)** — Added A* for targeted shortest paths
  and DFS for exploration/alternative routing, complementing the core BFS.
- **Score-log driven tuning** — `logs/scores.log` captures game outcomes
  for data-driven constant adjustment.
- **Refined H2H danger detection** — `HeadToHeadFilter` now only prunes
  moves into cells the enemy can *actually* reach (not walls or bodies).
- **Starvation override** — `MoveScorer` now prioritises contested food
  when HP is low, even if the H2H is risky, as mutual death is better than
  starvation.
- **Refined Food Strategy** — Snake is less 'greedy'. It now avoids risky
  food (corners, near enemies) unless starving, and prioritises guaranteed
  food over contested ones.

---

## 🟢 Next up

## 🟡 Cleanups / micro-perf

## 🔵 Personality & Tactical Personas (New)

These behaviors shift the bot from "survival only" to active playstyles.

### 1. ✅ The Territorial Bully (Aggressive Cornering)
*   **Behavior**: Minimize the space available to enemies.
*   **Implementation**: Smooth bonus for reducing enemy Voronoi area when they are getting cramped. Ramp up towards `OPPONENT_TRAP_BONUS`.
*   **Update**: Increased `BULLY_SCORE_FACTOR` (50.0) and `BULLY_AREA_THRESHOLD_MULT` (3) for higher aggression.
*   **Complexity**: **Medium**. Done via `SurvivalArea` and `AggressionScorer` update.

### 2. The Gatekeeper (Food/Path Blocking)
*   **Behavior**: Intercept enemies on their way to food.
*   **Implementation**: Identify high-value food for enemies (via BFS) and prioritize moves that put us on their shortest path if we can reach it first (and are longer).
*   **Complexity**: **Medium**. Done via BFS backtracking and `TurnContext` pre-calculation.

### 3. ✅ The Parasite (Enemy Tail Shadowing)
*   **Behavior**: Safely follow large snakes by sticking to their tails.
*   **Implementation**: Score moves that land adjacent to an enemy's tail (which will be empty next turn).
*   **Complexity**: **Low**. Done via `ParasiteScorer`.
*   **Verification**: Added to `run-local-cli.ps1` and `benchmark.ps1`.

### 4. The Hoarder (Resource Denial)
*   **Behavior**: Deny food to others even when healthy.
*   *   **Implementation**: Increase `foodWeight` specifically for food items that an enemy is also pursuing.
*   **Complexity**: **Low**. Conditional logic in `FoodScorer`.

### 5. ✅ The Duelist (H2H Hunter)
*   **Behavior**: Actively seek winning head-to-head collisions.
*   **Implementation**: Higher bonuses for moves that threaten a smaller snake's head.
*   **Complexity**: **Low**. Done via `DuelistScorer` and `HeadToHeadFilter` refinements.

---

## 🔴 High value, high risk — only with measurement

### 9. 2-ply look-ahead (alpha-beta on the top-N candidates)
One-ply will *always* mispredict trap-by-many-turns scenarios. A capped
2-ply (our move + best enemy reply) catches most of them.

- Reintroduce `Simulator` / `SimState` (deleted earlier for latency).
- Cap to top 2–3 of our candidates × top 2 enemy replies.
- **Mandatory wallclock guard** (e.g. `EngineConfig.timeBudgetRatio = 0.7`
  of `game.timeout()`).
- This is the change that previously pushed the bot to ~500 ms — only
  do it after #1–#5 above are exhausted, with logging proving one-ply
  is the bottleneck.

---

## 🧪 Cross-cutting

### Competitive Testing (A/B Testing)
Run your latest build against a known stable version on your own machine.
- Use `scripts/benchmark.ps1` to run automated games between different versions/personalities.
- Support for dynamic snake selection: `benchmark.ps1 -n Bully -p 8080 -n Duelist -p 8081`.
- Distinguish outcomes in `logs/scores.log` by passing unique names to each instance.

### Score-log driven tuning
The `score` logger now writes one line per game to `logs/scores.log` with
opponent names, mode, turns, lengths, survivors. Use this to:

- compare win-rate vs specific snakes after each change;
- detect regressions (e.g. "we now lose more vs longer snakes" → revisit
  the H2H aggression items).

### Tests (suggested once tuning fatigue hits)
A small `MoveScorer` golden-state suite with 3–4 hand-built `BoardGrid`s
(trap pocket, food race tied, food race we lose, open space) would let
you adjust constants confidently. ~50 lines total. No framework beyond
JUnit 5 needed.