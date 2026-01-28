package com.TaylorBros.ManhwaMod;

import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class SkillNamingEngine {
    private static final Random random = new Random();
    private static final Map<String, String[]> SHAPE_BUCKETS = new HashMap<>();

    static {
        // --- TIER 1: BASICS (NOUNS) ---
        SHAPE_BUCKETS.put("PUNCH", new String[]{
                "Impact", "Fist", "Smash", "Crush", "Strike", "Knuckle", "Blow", "Force", "Palm", "Drive"
        });
        SHAPE_BUCKETS.put("DASH", new String[]{
                "Flash", "Step", "Stride", "Rush", "Walk", "Drift", "Shift", "Velocity", "Flicker", "Sprint"
        });

        // --- TIER 2: MELEE ARTS (NOUNS) ---
        SHAPE_BUCKETS.put("SLASH", new String[]{
                "Cut", "Sever", "Blade", "Edge", "Fang", "Claw", "Rend", "Slash", "Arc", "Scythe"
        });
        SHAPE_BUCKETS.put("VERT_SLASH", new String[]{"Guillotine", "Drop", "Splitter", "Divide", "Cleave", "Fall", "Downstroke"});
        SHAPE_BUCKETS.put("HORIZ_SLASH", new String[]{"Sweep", "Horizon", "Crescent", "Cross", "Bisect", "Wave"});

        // --- TIER 3: PROJECTILES (NOUNS) ---
        SHAPE_BUCKETS.put("SINGLE", new String[]{"Bolt", "Shot", "Bullet", "Slug", "Needle", "Dart", "Piercer", "Arrow"});
        SHAPE_BUCKETS.put("BEAM", new String[]{"Cannon", "Ray", "Laser", "Stream", "Lance", "Spear", "Buster", "Pillar"});
        SHAPE_BUCKETS.put("BALL", new String[]{"Orb", "Star", "Sphere", "Core", "Sun", "Moon", "Nova", "Comet"});
        SHAPE_BUCKETS.put("RAY", new String[]{"Gleam", "Flash", "Vector", "Line", "Streak", "Trace"});

        // --- TIER 4: AOE / EXPLOSIVES (NOUNS) ---
        SHAPE_BUCKETS.put("CONE", new String[]{"Breath", "Roar", "Howl", "Spray", "Exhale", "Wave"});
        SHAPE_BUCKETS.put("IMPACT_BURST", new String[]{"Eruption", "Blast", "Explosion", "Burst", "Cataclysm", "Quake", "Shatter"});
        SHAPE_BUCKETS.put("FLARE", new String[]{"Signal", "Lotus", "Bloom", "Firework", "Beacon", "Radiance"});

        // --- TIER 5: UTILITY (NOUNS) ---
        SHAPE_BUCKETS.put("BOOMERANG", new String[]{"Disc", "Chakram", "Ring", "Halo", "Orbit", "Returner"});
        SHAPE_BUCKETS.put("WALL", new String[]{"Shield", "Aegis", "Barrier", "Fortress", "Gate", "Wall", "Dome"});
        SHAPE_BUCKETS.put("SPIKES", new String[]{"Thorns", "Spikes", "Needles", "Fangs", "Spears", "Garden", "Forest"});

        // --- TIER 6: HIGH SPEED (NOUNS) ---
        SHAPE_BUCKETS.put("BLINK_STRIKE", new String[]{"Ambush", "Assassination", "Shunpo", "Mirage", "Teleport", "Backstab"});

        // --- TIER 7: ULTIMATES (NOUNS) ---
        SHAPE_BUCKETS.put("BARRAGE", new String[]{"Storm", "Rain", "Hail", "Torrent", "Deluge", "Gatling", "Volley"});
        SHAPE_BUCKETS.put("BARRAGE_PUNCH", new String[]{"Pummel", "Rush", "Opa-Opa", "Machine-Gun", "Gatling-Gun"});
        SHAPE_BUCKETS.put("SLASH_BARRAGE", new String[]{"Dance", "Waltz", "Tempest", "Cyclone", "Shredder"});
        SHAPE_BUCKETS.put("RAIN", new String[]{"Downpour", "Shower", "Fall", "Judgment", "Descent"});
        SHAPE_BUCKETS.put("AOE", new String[]{"Domain", "Zone", "Field", "World", "Sanctuary", "Territory"});

        // --- ELEMENTS (ADJECTIVES) ---
        SHAPE_BUCKETS.put("FIRE", new String[]{"Infernal", "Blazing", "Solar", "Crimson", "Volcanic", "Burning", "Scorching", "Molten", "Red", "Flame"});
        SHAPE_BUCKETS.put("LAVA", new String[]{"Obsidian", "Magma", "Melting", "Ashen", "Erupting", "Fuming"});
        SHAPE_BUCKETS.put("WATER", new String[]{"Abyssal", "Tidal", "Azure", "Oceanic", "Liquid", "Flowing", "Deep", "Blue"});
        SHAPE_BUCKETS.put("ICE", new String[]{"Glacial", "Frozen", "Frost", "Arctic", "Frigid", "Polar", "Crystal", "White"});
        SHAPE_BUCKETS.put("ACID", new String[]{"Corrosive", "Toxic", "Caustic", "Vile", "Melting", "Eroding", "Green"});
        SHAPE_BUCKETS.put("LIGHTNING", new String[]{"Thunder", "Voltic", "Electric", "Galvanic", "Shocking", "Flash", "Ionic", "Lightning"});
        SHAPE_BUCKETS.put("WIND", new String[]{"Gale", "Storm", "Aerial", "Wind", "Aero", "Sonic", "Howling", "Swift"});
        SHAPE_BUCKETS.put("EARTH", new String[]{"Terra", "Ancient", "Heavy", "Stone", "Rock", "Seismic", "Titan"});
        SHAPE_BUCKETS.put("FORCE", new String[]{"Kinetic", "Gravity", "Heavy", "Crushing", "Absolute", "Repulsive"});
        SHAPE_BUCKETS.put("LIGHT", new String[]{"Sacred", "Holy", "Divine", "Luminous", "Heavenly", "Golden", "Purifying"});
        SHAPE_BUCKETS.put("SHADOW", new String[]{"Umbral", "Dark", "Grim", "Shadow", "Black", "Night", "Hidden"});
        SHAPE_BUCKETS.put("VOID", new String[]{"Null", "Empty", "Void", "Zero", "Endless", "Hollow", "Abyssal"});
        SHAPE_BUCKETS.put("POISON", new String[]{"Venomous", "Viral", "Plagued", "Noxious", "Deadly", "Purple", "Snake"});
        SHAPE_BUCKETS.put("NONE", new String[]{"Pure", "True", "Inner", "Martial", "Raw", "Focus", "Spirit"});

        // --- MODIFIERS (SUFFIXES / TITLES) ---
        // Rule: Must be a Noun (Stasis) or "of X" (of Silence). No Participles (Deflecting).

        SHAPE_BUCKETS.put("EXPLODE", new String[]{
                "of Destruction", "of Ruin", "Zero", "Burst", "Impact", "Crash", "Bomb", "Nova", "Wrecker"
        });
        SHAPE_BUCKETS.put("STUN", new String[]{
                "Stasis", "Lock", "Prison", "Bind", "Arrest", "Shock", "Freeze", "Stop", "Hold"
        });
        SHAPE_BUCKETS.put("LIFESTEAL", new String[]{
                "Eater", "Drain", "Siphon", "Absorb", "Leech", "Drinker", "Thief", "Devourer"
        });
        SHAPE_BUCKETS.put("WEAKEN", new String[]{
                "Breaker", "Crusher", "Erosion", "Rot", "Decay", "Cripple", "Bane"
        });
        SHAPE_BUCKETS.put("BOUNCE", new String[]{
                "of Deflection", "Ricochet", "Rebound", "Echo", "Cycle", "Return", "Reflection"
        });
        SHAPE_BUCKETS.put("CHAIN", new String[]{
                "Link", "Chain", "Web", "Network", "Spread", "Spark", "Arc"
        });
        SHAPE_BUCKETS.put("VAMPIRE", new String[]{
                "Nosferatu", "Vampire", "Blood-Letter", "Dracula", "Gore", "Feast"
        });
        SHAPE_BUCKETS.put("GRAVITY", new String[]{
                "Singularity", "Hole", "Vortex", "Magnet", "Star", "Well", "Crush"
        });
        SHAPE_BUCKETS.put("WITHER", new String[]{
                "of Death", "Reaper", "Dust", "Ash", "Blight", "Plague", "Curse"
        });
        SHAPE_BUCKETS.put("EXECUTE", new String[]{
                "Execution", "Guillotine", "Finisher", "Ender", "Terminator", "Sentence", "Death"
        });
        SHAPE_BUCKETS.put("NONE", new String[]{
                "Art", "Style", "Technique", "Form", "Method", "Step", "I", "II", "III", "IV"
        });
    }

    public static String getElementName(String tag) {
        if (!SHAPE_BUCKETS.containsKey(tag)) return "Mystic";
        String[] options = SHAPE_BUCKETS.get(tag);
        return options[random.nextInt(options.length)];
    }

    public static String getShapeName(String tag) {
        if (!SHAPE_BUCKETS.containsKey(tag)) return "Art";
        String[] options = SHAPE_BUCKETS.get(tag);
        return options[random.nextInt(options.length)];
    }

    public static String getModifierName(String tag) {
        if (!SHAPE_BUCKETS.containsKey(tag)) return "Technique";
        String[] options = SHAPE_BUCKETS.get(tag);
        return options[random.nextInt(options.length)];
    }
}