package sneak.snaek.engine.scorer;

/**
 * Central tuning knobs for {@link MoveScorer}.
 *
 * Kept package-private and grouped here so a single file shows the entire
 * "personality" of the bot. Magnitudes are sized relative to each other:
 *   TRAP_PENALTY (1000) ≫ FOOD_BONUS (500) > H2H_KILL_BONUS (200)
 *                        > STRETCH_BONUS (150) ≥ TAIL_BONUS (100)
 *                        ≈ CENTER_BONUS (100) > HAZARD_PENALTY (25)
 * — so survival overrides aggression overrides positional tiebreakers.
 */
public final class ScoringConstants {

    /** Maximum food-race weight. Sized to be the dominant *positional*
     *  signal among the bonuses (TAIL_BONUS=100, H2H_KILL_BONUS=200,
     *  CENTER_BONUS=100) so winnable food still pulls reliably — but
     *  capped at TRAP_PENALTY (1 000) so it can never coax us into a
     *  trap, and small enough that a clear survival-area difference
     *  (Voronoi-weighted, range 0…2·length) can override it when the
     *  food cell is in tight space. The lead dampener (see
     *  {@link MoveScorer#computeFoodWeight}) scales this down further
     *  as we pull ahead.  */
    public static final double FOOD_BONUS    = 750.0;

    /** Heavy penalty applied when the survival pocket (raw owned cells)
     *  is smaller than our body length — i.e. we'd self-collide before
     *  the tail vacates, or be sealed in by an enemy cutoff. Sized to
     *  beat any non-trapped alternative (max non-trapped survival ≈
     *  2·length ≈ 200) yet stay finite so the bot can still pick a
     *  trapped cell when *every* option is trapped (least-bad mode). */
    public static final double TRAP_PENALTY  = 2_000.0;

    /** Multiplier applied to the (non-trapped) Voronoi-weighted owned-area
     *  count. Survival is the only term that actually predicts
     *  trap-proximity, so area deltas between candidate moves must
     *  dominate the positional bonuses (STRETCH=150, CENTER=100,
     *  H2H=200 …). At 1×, a 20-cell drop in owned region — a clear
     *  warning sign that we're walking into enemy pressure — was
     *  routinely outweighed by a +50 stretch / centre swing. At 3× a
     *  20-cell delta is worth 60 pts, more than enough to override any
     *  combination of positional tiebreakers and steer toward the
     *  roomiest non-trapped option. Does NOT scale TRAP_PENALTY:
     *  trapped vs non-trapped contrast stays at the original 1 000-pt
     *  cliff, so the bot doesn't gain a perverse incentive to court
     *  near-trap states for marginally larger owned counts. */
    public static final double SURVIVAL_WEIGHT = 3.0;

    /** Max bonus for moving toward our own tail. Acts as a corridor-
     *  rescue tiebreaker — preventing the long-snake "wander into a
     *  U-shape and seal it" failure mode. Gated to only apply when the
     *  reachable pocket is genuinely tight (owned < TAIL_BONUS_AREA_MULT
     *  · myLength); in open space it would otherwise pull the snake
     *  into the smallest possible loop and self-coil to death. */
    public static final double TAIL_BONUS    = 100.0;

    /** Tail bonus applies only when owned-area is below this multiple of
     *  body length. Above the threshold there is plenty of room to
     *  survive without hugging our own body, and tail-adjacency pull
     *  becomes actively harmful (causes early-game self-coiling). */
    public static final int    TAIL_BONUS_AREA_MULT = 2;

    /** Max bonus for keeping a *long* BFS path between head and tail in
     *  open space — i.e. "stretch out / zig-zag" instead of coiling. The
     *  dual of TAIL_BONUS: tight pockets pull toward the tail (rescue),
     *  open space pushes away from it (stay extended → maximises future
     *  options and prevents early-game tail-orbit oscillation). Sized
     *  larger than CENTER_BONUS (100) so it can break out of a small
     *  central loop, smaller than H2H_KILL_BONUS (200) and FOOD_BONUS
     *  (500) so it never overrides aggression or food. Caps at the
     *  bonus value once {@code tailDist >= myLength} (body fully
     *  extended — no further benefit to walking away). */
    public static final double STRETCH_BONUS = 100;

    /** Length lead (over the longest live enemy) at which we start
     *  dampening the food bonus. At lead ≤ THRESHOLD the chase is at
     *  full FOOD_BONUS; past it the weight decays as 1/(1+excess) —
     *  see {@link MoveScorer#computeFoodWeight}. Larger ⇒ stay
     *  aggressive longer. */
    public static final int    LEAD_THRESHOLD = 4;

    /** HP threshold below which food urgency starts ramping up. At
     *  exactly this value the food multiplier is 1× (unchanged); each
     *  HP below adds a linear share until the multiplier doubles at
     *  HP=0. Combined with {@link #HAZARD_HP_BUFFER} this gives a
     *  starving snake — especially one stuck in hazard — a strong
     *  pull toward food it would otherwise pass on while leading on
     *  length. Sized at 60 (rather than 50) so the urgency ramp
     *  *starts* before the classic "60-HP danger zone" rather than
     *  reacting to it. */
    public static final int    HEALTH_URGENCY_THRESHOLD = 60;

    /** Effective HP drop applied to the urgency calculation when ANY
     *  segment of our body is currently inside a hazard cell (this
     *  variant drains HP based on body presence, not just the head).
     *  The next turns will keep draining HP at HAZARD_PENALTY-equivalent
     *  rate as long as we stay touching the sauce, so we should act as
     *  if we were already that much weaker — pre-empting the cliff
     *  rather than reacting to it. Roughly 1–2 turns of hazard drain
     *  (≈15 HP/turn in default Royale). */
    public static final int    HAZARD_HP_BUFFER = 20;

    /** Score cost per body segment that will be inside a hazard cell
     *  after our move (head + every non-vacating body cell). Hazard
     *  drains HP based on body presence, not just the head, so a long
     *  snake stretched through sauce bleeds HP every turn for every
     *  segment in it. The score units approximate the "true HP drain
     *  per turn" that move commits us to:
     *    no segments in hazard  → 0
     *    head only              → 1×           (was the old single-step fee)
     *    head + 4 body segments → 5×
     *  Sized at 250 — large enough that a clean alternative beats a
     *  multi-segment hazard exposure decisively, but a 4-segment
     *  exposure (1 000) still stays under TRAP_PENALTY (2 000) so the
     *  trapped/non-trapped cliff remains the dominant signal. The
     *  vacating tail (if {@code next} isn't food) is correctly excluded
     *  — we *gain* by leaving a hazard cell behind. */
    public static final double HAZARD_PENALTY = 250.0;

    /** Weight of hazard cells when counting the survival/flood-fill area.
     *  Hazards are passable but drain HP, so a pocket made mostly of
     *  hazard cells is effectively smaller than its raw cell count. */
    public static final double HAZARD_AREA_WEIGHT = 0.25;

    /** Bonus for moves that land adjacent to a strictly shorter enemy's head.
     *  We'd win that head-to-head if they step into us — and even if they
     *  avoid the cell, we've taken away a legal move from them, sometimes
     *  forcing them into a wall or their own body. Sized between TAIL_BONUS
     *  (100) and FOOD_BONUS (500): meaningful in open space, won't
     *  override food or trap detection.
     *
     *  Effective bonus is divided by max(1, enemies.size()) at scoring
     *  time — a fight in a 1v1 is worth full value, but in an 8-player
     *  Battle Royale the same fight invites a third snake to capitalise
     *  on whatever cell-cluster you've both committed to. Late-game
     *  attrition will naturally restore the full value as the field
     *  thins out. */
    public static final double H2H_KILL_BONUS = 200.0;

    /** Bonus for trapping an opponent (reducing their Voronoi-owned area
     *  below their body length). Being the one who "seals the deal" on
     *  an enemy's survival is high value. Sized significantly (600)
     *  to outweigh a food chase or a comfortable length lead, but still
     *  below TRAP_PENALTY (2000) so we don't suicide to kill.
     *  Like H2H_KILL_BONUS, it scales down in multi-snake games. */
    public static final double OPPONENT_TRAP_BONUS = 600.0;

    /** Multiplier for each cell we "take away" from an enemy's Voronoi
     *  area when they are already getting cramped (area < 3*length).
     *  Creates a smooth gradient of aggression that ramps up to the
     *  OPPONENT_TRAP_BONUS. Sized so that squeezing an enemy by 10
     *  cells is worth 500 pts (comparable to FOOD_BONUS). */
    public static final double BULLY_SCORE_FACTOR = 50.0;

    /** Area threshold multiplier for the Bully personality. We start
     *  applying the BULLY_SCORE_FACTOR when an enemy's Voronoi area
     *  drops below this multiple of their length. */
    public static final int    BULLY_AREA_THRESHOLD_MULT = 3;

    /** Soft preference for cells closer to the board centre. Edges are
     *  more easily cut off (a wall already blocks one side). Sized small
     *  so it never overrides survival, food, h2h pressure, or tail-chase
     *  in late-game tight loops — purely a mid-game positional tiebreaker
     *  when other terms are flat. */
    public static final double CENTER_BONUS = 100.0;

    /** Hard penalty per wall the candidate cell physically touches.
     *  Edge cell ⇒ −50, corner cell ⇒ −100. Complements the soft
     *  {@link #CENTER_BONUS} gradient: the gradient is too weak (≈16 at
     *  edges) to break the common "circle along the border" failure
     *  mode where one wall blocks one direction and our body blocks
     *  another, leaving only two ways to be cut off. Sized below
     *  H2H_KILL_BONUS (200), FOOD_BONUS (500) and STRETCH_BONUS (150)
     *  so we can still chase kills/food/space onto a wall when needed,
     *  but pays for itself over a multi-turn border loop. Far below
     *  TRAP_PENALTY (1000) — never forces a self-trap. */
    public static final double WALL_PENALTY = 50.0;

    /** Penalty for food items in risky positions (corners or next to enemies).
     *  A corner food item is one that touches two walls.
     *  An "enemy-adjacent" food item is one that is a neighbor of an enemy head.
     *  Sized significantly to discourage "greedy" moves into bad spots unless
     *  starving. */
    public static final double RISKY_FOOD_PENALTY = 300.0;

    private ScoringConstants() {}
}

