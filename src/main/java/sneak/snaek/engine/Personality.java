package sneak.snaek.engine;

public enum Personality {
    BULLY,
    MIDAS,
    TURTLE,
    PARASITE,
    DUELIST;

    public static Personality fromString(String s) {
        if (s == null) return BULLY;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BULLY;
        }
    }
}
