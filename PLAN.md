# super-sneyk — Improvement Plan

Ordered by **(benefit − complexity)**. Items 1 & 2 below the line are
already implemented (kept here for context). Tackle from the top.

---

## ✅ Done

- **Hoist enemy BFS out of the per-move loop** — `MoveScorer.computeEnemyReach`
  is called once per turn in `SnakeEngine.move()` and reused for every
  candidate. Cuts per-turn BFS by ~33%.
- **BFS-based tail distance** — `MoveScorer` now uses path distance to the
  tail (via the `myDist` array already computed for the candidate cell)
  instead of Manhattan, with a fallback to the tail's nearest reachable
  neighbour when the tail cell itself is still blocked (post-eat).

---

## 🟢 Next up

### 1. Hazard awareness  *(15 min, mode-defensive)*
`BoardGrid.isHazard()` exists but `MoveScorer` ignores it. In Royale,
hazard cells drain ~15 HP/turn — the bot will happily walk a hazard
corridor next to a clean route and starve.

- Add `HAZARD_PENALTY ≈ 5` per cell when `next` is a hazard.
- Optional: inflate BFS edge weight through hazards (turns BFS into a
  cheap weighted search; only matters if hazard zones are common).

### 2. Refine `avoidHeadToHeadLosses` with enemy legal moves  *(10 min)*
Currently marks **all four** neighbours of every equal/longer enemy as
dangerous. Enemies cannot:

- move into a wall;
- move into their own neck or any snake body.

Filter the danger set with `!grid.isBlocked(neighbor)` before adding.
Effect: less paranoid in tight quarters; opens up moves we currently
veto wrongly.

### 3. Boolean-array flood fill  *(15-line rewrite, pure perf)*
`FloodFill.floodFill` (if still present) uses `HashSet<Coord>`. Rewrite
with `boolean[w][h]` to match `bfsWithOwners`. ~5× faster, less GC.
**Note:** after the BFS hoist, `FloodFill` may already be dead code —
verify references before doing this; possibly just delete the file.

### 4. Win-the-H2H bonus against shorter enemies  *(15 lines)*
We avoid losing H2Hs but never seek H2Hs we'd win. Add a small bonus
(~50–200) when `next` is also a neighbour of a strictly shorter enemy's
head. Real meta in duels — pressures smaller enemies into bad cells.
No new search — uses data we already have.

### 5. Starvation override  *(4 lines)*
The lead dampener tunes food *down* when ahead, but there's no symmetric
*up* signal when starving. If `health < md + 5`, treat food as winnable
even at `md == ed` regardless of length (mutual death is no worse than
starvation). Prevents preventable starvation deaths.

---

## 🟡 Cleanups / micro-perf

### 6. Eliminate `Coord` allocation in BFS inner loop
`CoordUtils.neighbors(c)` returns 4 fresh `Coord` records per visited
cell — thousands of tiny allocations per turn. Inline `(±1, 0), (0, ±1)`
deltas as `int x, y`. Modest GC reduction; JIT eats most of the cost
already so low priority.

### 7. Cache enemy lengths into an `int[]`
After `computeEnemyReach`, also build
`int[] enemyLengths = enemies.stream().mapToInt(BattleSnake::length).toArray()`
so the food H2H check is an array lookup rather than `enemies.get(i).length()`.
Trivial; useful only when iterating many cells.

### 8. Delete `PathOptions.randomMove()`
Dead code — never called.

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