# super-sneyk

A [BattleSnake](https://play.battlesnake.com) bot written in Java 21. No web framework — just `com.sun.net.httpserver.HttpServer`, Gson for JSON, and SLF4J/Logback for logs.

The decision logic is intentionally **simple, fast, and one-ply**: for every legal move, score it once, pick the highest. No minimax, no game-tree search. Average decision time is ~1 ms, well under the 500 ms BattleSnake budget.

---

## How a turn is decided

When BattleSnake POSTs `/move` with the current game state, the bot runs three steps in [`SnakeEngine.move()`](src/main/java/sneak/snaek/SnakeEngine.java):

1. **Safety filter** — eliminate moves that are immediately suicidal:
   - off the board
   - into any snake's body (with the "tail vacates next turn" rule respected)
   - into a square an equal-or-longer enemy could also reach this turn (we'd lose the head-to-head)

2. **Hoist per-turn data** — compute things that are the same for every candidate move once, not three times:
   - **Enemy reach map** — multi-source BFS from every enemy head: how many steps each cell is from the closest enemy, and *which* enemy that is.
   - **Food weight** — how aggressively to chase food this turn (drops as our length lead grows).

3. **Score every remaining move** with [`MoveScorer.score()`](src/main/java/sneak/snaek/strategy/MoveScorer.java) and pick the highest. Each move's score breakdown is logged so you can see *why* it was picked.

If only one move survives the safety filter, it's returned immediately.

---

## The scoring function — plain English

For a candidate move, we'd land on a cell called `next`. From `next` we run a single BFS to know the shortest path distance to every other cell. Then we add up:

| Term | What it asks | Sign |
|---|---|---|
| **Survival (Voronoi area)** | "How much room do I actually own after the move?" | + |
| **Trap penalty** | "Is that room smaller than my body?" | − |
| **Hazard penalty** | "Did I just step on hot lava?" | − |
| **H2H aggression** | "Am I threatening a smaller snake's head?" | + |
| **Tail-chase bonus** | "Am I following my own tail safely?" | + |
| **Centre bonus** | "Did I move toward the middle, away from walls?" | + |
| **Food bonus** | "Will I reach a food before any enemy can?" | + |

The final score is just the sum. Highest wins.

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

## Why no game-tree search?

A previous version of this bot did 2-ply minimax with Voronoi and several heuristics. It was strong but hit ~500 ms — right at the response budget cliff. The current bot:

- Uses **2 BFS per turn** (one from each candidate cell + one from all enemies, hoisted).
- Runs in well under 5 ms even on a 25×25 board.
- Catches most "trap" mistakes via the Voronoi survival term — the same insight a 2-ply search would give us, without simulating moves.

See [`PLAN.md`](PLAN.md) for the ranked list of further improvements (and which ones to avoid without measurement).

---

## Build & run

```bash
mvn package
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The bot listens on **port 8080** by default. Optional CLI args let you run multiple instances side-by-side:

```bash
java -jar target/...jar 8080 "Snake-A" "#ff9900"
java -jar target/...jar 8081 "Snake-B" "#00aaff"
```

See [`AGENTS.md`](AGENTS.md) for build quirks, deployment tunnels, and conventions for AI coding agents working on this codebase.

---

## Logs

Two log streams (configured in [`logback.xml`](src/main/resources/logback.xml)):

- **Application log → console.** Every turn prints a one-line summary with the chosen move and full score breakdown:
  ```
  Turn 137 | Head=Coord[x=6, y=5] Health=100 | Move=RIGHT total=541.3 survival=84.0 food=500.0 tail=14.3 h2h=0.0 centre=25.0 hazard=-0.0 trapped=false owned=156 (0ms)
  ```
  Set Logback to `DEBUG` on `sneak.snaek.SnakeEngine` to also see the breakdown for every candidate move (not just the chosen one).

- **Battle outcomes → `logs/scores.log`.** One line per game with result, opponent names, mode, turn count, and survivors. Useful for tracking win-rate per opponent over time.

---

## Repo layout

| Path | What it is |
|---|---|
| `sneak.snaek.FierceBattleSnakeApplication` | HTTP server + request routing |
| `sneak.snaek.SnakeEngine` | Decision orchestrator (safety filter + scoring loop) |
| `sneak.snaek.strategy.MoveScorer` | All scoring logic & tunable constants live here |
| `sneak.snaek.board.BoardGrid` | Board representation (blocked cells, hazards, food) |
| `sneak.snaek.board.CoordUtils` | Coord arithmetic, neighbours, Manhattan distance |
| `sneak.snaek.model.*` | Immutable Java records for BattleSnake JSON API |

To **tune behaviour**, all knobs are constants at the top of `MoveScorer` — each one is documented with its role, scale, and rationale.

