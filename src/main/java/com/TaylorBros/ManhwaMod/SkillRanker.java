package com.TaylorBros.ManhwaMod;

public class SkillRanker {

    public enum Rank {
        F("F", 0xFFAAAAAA),       // Gray
        E("E", 0xFFFFFFFF),       // White
        D("D", 0xFF55FF55),       // Green
        C("C", 0xFF55FFFF),       // Aqua
        B("B", 0xFFAA00AA),       // Purple
        A("A", 0xFFFFAA00),       // Gold
        S("S", 0xFFFF5555),       // Red
        SS("SS", 0xFFAA0000),     // Dark Red
        SSS("SSS", 0xFF000000);   // Black (Glitch/God)

        public final String label;
        public final int color;

        Rank(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    // --- MAIN FORMULA ---
    public static Rank getRank(String recipe) {
        if (recipe == null || recipe.isEmpty()) return Rank.F;

        // Clean recipe (remove name part)
        String clean = recipe.contains("|") ? recipe.split("\\|")[0] : recipe;
        String[] parts = clean.split(":");
        if (parts.length < 3) return Rank.F;

        int shapeTier = getShapeTier(parts[0]);
        int elemTier = getElementTier(parts[1]);
        int modTier = getModifierTier(parts[2]);

        // The Average Logic
        double average = (shapeTier + elemTier + modTier) / 3.0;

        // SCORING THRESHOLDS
        if (average >= 5.5) return Rank.SSS; // Needs almost perfect roll (e.g. 6, 6, 5)
        if (average >= 5.0) return Rank.SS;
        if (average >= 4.5) return Rank.S;
        if (average >= 4.0) return Rank.A;
        if (average >= 3.0) return Rank.B;
        if (average >= 2.0) return Rank.C;
        if (average >= 1.5) return Rank.D;
        return Rank.E;
    }

    public static int getColor(String recipe) {
        return getRank(recipe).color;
    }

    // --- TIER VALUES (1 = Weak, 6 = Godly) ---

    private static int getShapeTier(String s) {
        return switch (s) {
            case "PUNCH", "DASH" -> 1;
            case "SLASH", "VERT_SLASH", "HORIZ_SLASH" -> 2;
            case "SINGLE", "BEAM", "BALL", "RAY" -> 3;
            case "CONE", "IMPACT_BURST", "FLARE" -> 4;
            case "BOOMERANG", "WALL", "SPIKES" -> 4;
            case "BLINK_STRIKE" -> 5;
            // The "Ultimate" shapes are Tier 6
            case "BARRAGE", "BARRAGE_PUNCH", "SLASH_BARRAGE", "RAIN", "AOE" -> 6;
            default -> 1;
        };
    }

    private static int getElementTier(String e) {
        return switch (e) {
            case "NONE", "EARTH", "WATER", "WIND" -> 1;
            case "FIRE", "ICE", "ACID", "POISON" -> 2;
            case "LIGHTNING", "LAVA", "FORCE" -> 3;
            case "LIGHT", "SHADOW" -> 4;
            case "VOID" -> 6; // VOID is the only Tier 6 element
            default -> 1;
        };
    }

    private static int getModifierTier(String m) {
        return switch (m) {
            case "NONE", "BOUNCE" -> 1;
            case "STUN", "WEAKEN", "CHAIN" -> 2;
            case "EXPLODE", "LIFESTEAL", "GRAVITY" -> 4;
            case "VAMPIRE", "WITHER" -> 5;
            case "EXECUTE" -> 6; // EXECUTE is the only Tier 6 modifier
            default -> 1;
        };
    }
}