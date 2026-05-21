# super-sneyk

A [BattleSnake](https://play.battlesnake.com) bot written in Java 21. No web framework — just `com.sun.net.httpserver.HttpServer`, Gson for JSON, and SLF4J/Logback for logs.

The decision logic is a **modular pipeline of filters and scorers**, with optional **2-ply lookahead** (our move + best enemy reply) for high-stakes games. It balances deep survival analysis with a strict time budget (targeting <50ms per decision), well under the 500ms BattleSnake budget.

---

## How a turn is decided

When BattleSnake POSTs `/move` with the current game state, the bot runs the following steps in [`ModularSnakeEngine.move()`](src/main/java/sneak/snaek/engine/ModularSnakeEngine.java):

1. **Safety filters** — Prune moves that would cause immediate death (OOB, body collision, or losing a head-to-head against a longer snake).
2. **Turn Context** — Compute shared data once per turn (grid state, enemy reach map via multi-source BFS, food weights, and hazard status).
3. **1-Ply Scoring** — For each surviving candidate move, calculate a score based on Survival Area (Voronoi), Food distance, Tail proximity, and Aggression.
4. **2-Ply Lookahead** (Optional) — If enabled and time allows, simulate the top-N candidate moves and the most likely enemy responses to pick the move with the best projected outcome.

If only one move survives the safety filter, it's returned immediately.

---

## The scoring function — plain English

For a candidate move, we land on a cell called `next`. From `next` we run a single BFS to calculate distances to all reachable cells. Then we sum all active scorers:

| Term | What it asks | Personality |
|---|---|---|
| **Survival (Voronoi area)** | "How much room do I actually own after the move?" | ALL |
| **Trap penalty** | "Is that room smaller than my body?" | ALL |
| **Hazard penalty** | "Did I just step on hot lava?" | ALL |
| **H2H aggression** | "Am I threatening a smaller snake's head?" | BULLY |
| **Tail-chase bonus** | "Am I following my own tail safely?" | TURTLE |
| **Centre bonus** | "Did I move toward the middle, away from walls?" | ALL |
| **Food bonus** | "Will I reach a food before any enemy can?" | MIDAS |
| **Duelist bonus** | "Am I seeking head-to-head combat?" | DUELIST |
| **Parasite bonus** | "Am I shadowing a larger snake's tail?" | PARASITE |

The final score is a weighted sum based on the active **Personality**.

### 1. Survival — Voronoi area (the most important term)

A naive flood-fill counts *all* cells reachable from `next`. That's wrong: an enemy can reach some of those cells faster than we can. They might cut us off at a corridor and seal us in.

Instead, for every reachable cell we ask: **does our BFS distance to it beat the closest enemy's BFS distance?** If yes, the cell is "ours". This is a **Voronoi region** — the cells we win the race to. We count them.

- If we own **fewer cells than our body length** → `trapped = true`. We'd hit our own tail or be sealed in. Heavy penalty (`-1000`), and food chasing is *disabled* (food in a death pocket is bait).
- Otherwise → score the area, but cap at `2 × length` so an "infinitely safe" pocket doesn't drown out the smaller bonuses below.
- Hazard cells count as 25% of a normal cell — they're passable but drain HP, so a pocket made of them is effectively smaller.

This single change catches most "I sealed myself in" deaths without needing any look-ahead.

### 2. Trap penalty
A constant `−1000` when the Voronoi pocket is too small. Big enough to lose to any non-trapped alternative, but **finite** — when *every* move is trapped (rare end-game spirals), we still pick the largest pocket rather than freezing.

### 3. Hazard penalty
Stepping into a hazard cell costs `−25` (≈1.7× the default 15-HP drain in Royale). Small enough that food can still pull us through hazards, big enough that a clean alternative beats them.

### 4. H2H aggression
If `next` is one cell away from a **strictly shorter** enemy's head, we get `+200`. Either:
- The enemy steps into us → we eat them (we win H2H).
- The enemy avoids that cell → we just removed one of their three legal moves, possibly forcing them into a wall.

Either outcome is good for us. Disabled when we're already trapped — survival comes first.

### 5. Tail-chase bonus
Up to `+100`, decaying with the BFS distance from `next` to our own tail. In open space, all "safe" moves look identical (Voronoi area is capped). The tail-chase term breaks the tie by pulling us along our body, so a long snake naturally coils into a stable loop instead of wandering into a U-shape and sealing it.

### 6. Centre bonus
Up to `+100` at the dead centre, decaying as `1/(1+chebyshev_distance)`. Edges are riskier — a wall already blocks one side, so it's easier to get cut off there.

### 7. Food bonus
Up to `+1000 / (1 + my_distance)`, *only* if there exists a food cell where:
- our BFS distance is **strictly less** than every enemy's BFS distance, **OR**
- equal distance, but we're strictly longer than the contesting enemy (we'd win the H2H if we both arrived).

Equal-length ties are skipped — both snakes die on a mutual H2H.

The bonus weight smoothly decays once we're more than `4` longer than the longest enemy: at lead 4 it's 1000, at lead 5 it's 500, at lead 6 it's 333, etc. We slow down on food once we're comfortably ahead, but never stop entirely. In **solo play** (no enemies) the dampener is bypassed — growing is the only goal.

---

## Why the hybrid approach?

The bot uses a **1-ply greedy** foundation with **Voronoi area control** because it catches most mistakes (like sealing oneself in) without the overhead of deep search.

However, for competitive play (DUEL or ROYALE), it upgrades to **2-ply lookahead** (minimax-lite):
- **Predicts** enemy moves based on center-pull and collision avoidance.
- **Simulates** the joint move state.
- **Re-scores** the resulting board to detect traps that only appear several turns ahead.

This hybrid approach keeps the bot extremely fast (~1ms for 90% of turns) while providing the tactical depth needed for high-level play.

See [`PLAN.md`](PLAN.md) for the ranked list of further improvements (and which ones to avoid without measurement).

---

## Build & run

```bash
mvn package
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar
```

See [**TESTING.md**](TESTING.md) for a detailed guide on local testing, including snapshot tests and local simulations.

The bot listens on **port 8080** by default. Optional CLI args let you run multiple instances side-by-side:

```bash
java -jar target/...jar 8080 "Snake-A" "#ff9900"
java -jar target/...jar 8081 "Snake-B" "#00aaff"
```

See [`AGENTS.md`](AGENTS.md) for build quirks, deployment tunnels, and conventions for AI coding agents working on this codebase.

---

## Logs

Two log streams (configured in [`logback.xml`](src/main/resources/logback.xml)):

- **Application log → console.** Every turn prints a one-line summary with the chosen move and full score breakdown.
- **Decision log → `logs/decisions.log`.** Detailed breakdown of every candidate move, including filter results and individual scorer outputs.
- **Battle outcomes → `logs/scores.log`.** One line per game with result, opponent names, mode, turn count, and survivors.

---

## Repo layout

| Path | What it is |
|---|---|
| `sneak.snaek.FierceBattleSnakeApplication` | HTTP server + request routing |
| `sneak.snaek.engine.ModularSnakeEngine` | Decision orchestrator (filters + scoring loop + lookahead) |
| `sneak.snaek.engine.scorer.Scorer` | Individual scoring components (Survival, Food, etc.) |
| `sneak.snaek.board.BoardGrid` | Board representation (blocked cells, hazards, food, tail-vacate) |
| `sneak.snaek.model.*` | Immutable Java records for BattleSnake JSON API |

To **tune behaviour**, all knobs are constants in `sneak.snaek.engine.scorer.ScoringConstants` — each one is documented with its role, scale, and rationale.

