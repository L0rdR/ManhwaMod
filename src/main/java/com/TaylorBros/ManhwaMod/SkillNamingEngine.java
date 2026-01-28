package com.TaylorBros.ManhwaMod;

import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class SkillNamingEngine {
    private static final Random random = new Random();
    private static final Map<String, String[]> SHAPE_BUCKETS = new HashMap<>();

    static {
        // --- TIER 1: BASICS ---
        SHAPE_BUCKETS.put("PUNCH", new String[]{
                "Strike", "Impact", "Fist", "Blow", "Knuckle", "Hammer", "Force", "Pressure",
                "Smash", "Jab", "Crush", "Drive", "Clench", "Grip", "Beatdown", "Vault", "Pummel", "Thrust", "Heavy"
        });
        SHAPE_BUCKETS.put("DASH", new String[]{
                "Step", "Stride", "Burst", "Flash", "Vault", "Shift", "Sprint", "Charge",
                "Slide", "Momentum", "Glide", "Surge", "Velocity", "Trail", "Flicker", "Rush", "Drift", "Blink"
        });

        // --- TIER 2: MELEE ARTS ---
        String[] bladedWords = {
                "Slash", "Edge", "Cutter", "Blade", "Cleave", "Scythe", "Fang", "Claw",
                "Shear", "Rip", "Laceration", "Divide", "Arc", "Splitter", "Sever", "Gash", "Point", "Steel"
        };
        SHAPE_BUCKETS.put("SLASH", bladedWords);
        SHAPE_BUCKETS.put("VERT_SLASH", new String[]{"Vertical", "Downfall", "Heaven-Splitter", "Gravity", "Descent", "Drop", "Plunge"});
        SHAPE_BUCKETS.put("HORIZ_SLASH", new String[]{"Horizon", "Sweep", "Round", "Circle", "Periphery", "Orbit", "Panorama"});

        // --- TIER 3: PROJECTILES ---
        SHAPE_BUCKETS.put("SINGLE", new String[]{"Bolt", "Bullet", "Dart", "Needle", "Shot", "Slug", "Stinger", "Arrow", "Pierce"});
        SHAPE_BUCKETS.put("BEAM", new String[]{"Beam", "Ray", "Laser", "Stream", "Current", "Flow", "Thread", "Lance", "Pillar", "Light"});
        SHAPE_BUCKETS.put("BALL", new String[]{"Orb", "Sphere", "Globe", "Core", "Sun", "Star", "Bubble", "Nova", "Planet", "Egg"});
        SHAPE_BUCKETS.put("RAY", new String[]{"Radiance", "Line", "Spectrum", "Vector", "Flash", "Streak", "Trace"});

        // --- TIER 4: AOE / EXPLOSIVES ---
        SHAPE_BUCKETS.put("CONE", new String[]{"Fan", "Spread", "Breath", "Howl", "Wave", "Shout", "Expanse", "Spray"});
        SHAPE_BUCKETS.put("IMPACT_BURST", new String[]{"Eruption", "Explosion", "Cataclysm", "Bang", "Detonation", "Blast", "Shockwave"});
        SHAPE_BUCKETS.put("FLARE", new String[]{"Corona", "Glow", "Signal", "Ignition", "Spark", "Bloom", "Radiance"});

        // --- TIER 5: UTILITY ---
        SHAPE_BUCKETS.put("BOOMERANG", new String[]{"Return", "Loop", "Orbit", "Cycle", "Rebound", "Spiral", "Helix"});
        SHAPE_BUCKETS.put("WALL", new String[]{"Barricade", "Fortress", "Shield", "Gate", "Barrier", "Bastion", "Screen", "Aegis"});
        SHAPE_BUCKETS.put("SPIKES", new String[]{"Thorns", "Needles", "Quills", "Fracture", "Tusk", "Pillar", "Bristle"});

        // --- TIER 6: HIGH SPEED ---
        SHAPE_BUCKETS.put("BLINK_STRIKE", new String[]{"Dimension", "Teleport", "Shadow", "Ghost", "Void", "Instant", "Phantom"});

        // --- TIER 7: ULTIMATES ---
        String[] barrageWords = {"Barrage", "Storm", "Rain", "Hail", "Thousand", "Infinite", "Onslaught", "Carnage", "Massacre", "Endless"};
        SHAPE_BUCKETS.put("BARRAGE", barrageWords);
        SHAPE_BUCKETS.put("BARRAGE_PUNCH", new String[]{"Gatling", "Machine", "Rapid-Fire", "Centillion", "God-Hand"});
        SHAPE_BUCKETS.put("SLASH_BARRAGE", new String[]{"Dance", "Execution", "Purgatory", "Nightmare", "Flash-Cut"});
        SHAPE_BUCKETS.put("RAIN", new String[]{"Downpour", "Heavenly", "Falling", "Meteor", "Drop", "Cascade"});
        SHAPE_BUCKETS.put("AOE", new String[]{"Domain", "Zone", "Field", "Atmosphere", "World", "Sanctuary"});

        // --- FIRE & LAVA ---
        SHAPE_BUCKETS.put("FIRE", new String[]{"Infernal", "Blazing", "Cinder", "Magma", "Solar", "Crimson", "Hellish", "Volcanic", "Phoenix", "Burning", "Ignited", "Scorching", "Molten", "Searing", "Pyre", "Flame", "Promethean", "Hell-Fire", "Supernova", "Ember"});
        SHAPE_BUCKETS.put("LAVA", new String[]{"Obsidian", "Magmatic", "Foundry", "Crust", "Molten", "Melting", "Core", "Tectonic", "Sulfuric", "Basalt", "Caldera", "Igneous", "Flowing", "Melt", "Ashen"});

        // --- WATER & ICE & ACID ---
        SHAPE_BUCKETS.put("WATER", new String[]{"Abyssal", "Tidal", "Deep", "Azure", "Hydric", "Mist", "Oceanic", "Tsunami", "Ripple", "Liquid", "Flowing", "Poseidon", "Current", "Drowning", "Aqua"});
        SHAPE_BUCKETS.put("ICE", new String[]{"Glacial", "Frozen", "Frost", "Crystal", "Arctic", "Subzero", "Boreal", "Winter", "Frigid", "Polar", "Permafrost", "Hail", "Ice-Bound", "Diamond", "Zero"});
        SHAPE_BUCKETS.put("ACID", new String[]{"Corrosive", "Toxic", "Caustic", "Vitriol", "Melting", "Dissolving", "Burning", "Vile", "Pungent", "Green", "Oozing", "Bile", "Miasmic", "Hazard", "Venomous"});

        // --- LIGHTNING & WIND ---
        SHAPE_BUCKETS.put("LIGHTNING", new String[]{"Volt", "Thunder", "Static", "Electric", "Plasma", "Bolt", "Galvanic", "Shining", "Shock", "Current", "Ion", "Radiant", "Spark", "Flash", "Keraunos", "Voltage", "Energy", "Discharge", "Jupiter", "Crackling"});
        SHAPE_BUCKETS.put("WIND", new String[]{"Gale", "Zephyr", "Storm", "Cyclone", "Ethereal", "Skyborne", "Aerial", "Aero", "Draft", "Whirlwind", "Cloud", "Tempest", "Spirit", "Hurricane", "Tornado", "Sonic", "Atmospheric", "Ventus", "Jet", "Vacuum"});

        // --- EARTH & FORCE ---
        SHAPE_BUCKETS.put("EARTH", new String[]{"Terra", "Lithic", "Quaking", "Ancient", "Primal", "Granite", "Iron", "Stone", "Mountain", "Dust", "Sand", "Crushing", "Fossil", "Geological", "Tectonic", "Amber", "Bedrock", "Gaia", "Solid", "Boulder"});
        SHAPE_BUCKETS.put("FORCE", new String[]{"Kinetic", "Gravity", "Impact", "Pressure", "Weight", "Momentum", "Crushing", "Singularity", "Heavy", "Massive", "Repelling", "Attracting", "Absolute", "Newtonian", "God-Strength"});

        // --- LIGHT, SHADOW & VOID ---
        SHAPE_BUCKETS.put("LIGHT", new String[]{"Radiant", "Holy", "Sacred", "Solar", "Divine", "Luminous", "Heavenly", "Angelic", "Golden", "Aura", "Glinting", "Saintly", "Purifying", "Blessed", "Resplendent"});
        SHAPE_BUCKETS.put("SHADOW", new String[]{"Umbral", "Dark", "Grim", "Night", "Obscure", "Shaded", "Abyssal", "Wicked", "Silent", "Stealthy", "Vampiric", "Nocturnal", "Gloomy", "Murky", "Eclipse"});
        SHAPE_BUCKETS.put("VOID", new String[]{"Null", "Empty", "Existence-Erasure", "Universal", "Eternal", "Zero", "Oblivion", "Chaos", "Dark-Matter", "Non-Existent", "Rift", "Abyssal", "End", "Infinity", "Cosmic"});

        // --- POISON & NONE ---
        SHAPE_BUCKETS.put("POISON", new String[]{"Venomous", "Toxic", "Blighted", "Viral", "Septic", "Noxious", "Deadly", "Fatal", "Malicious", "Decaying", "Festering", "Snake", "Viper", "Plague", "Infected"});
        SHAPE_BUCKETS.put("NONE", new String[]{"Pure", "Raw", "Mana", "Spirit", "Energy", "Basic", "Simple", "Awakened", "Neutral", "Focus", "Soul", "Inner", "Martial", "Striking", "Physical"});

        // --- MODIFIERS: COMBAT EFFECTS ---
        SHAPE_BUCKETS.put("EXPLODE", new String[]{
                "of Destruction", "Burst", "Eruption", "Cataclysm", "of the End", "Detonation",
                "of Chaos", "Shattering", "of Ruin", "Blast", "of Doom", "Nova", "Disintegration"
        });
        SHAPE_BUCKETS.put("STUN", new String[]{
                "of Paralysis", "Stasis", "Shock", "of the Gorgon", "Freezing", "Impact",
                "of Stillness", "Numbing", "of the Medusa", "Dazing", "of Inertia", "Locked"
        });
        SHAPE_BUCKETS.put("LIFESTEAL", new String[]{
                "of Recovery", "Leeching", "Drain", "Harvesting", "of Vitality", "Siphoning",
                "Consumer", "Life-Bound", "of Rebirth", "Healing", "Absorbing", "Marrow"
        });
        SHAPE_BUCKETS.put("WEAKEN", new String[]{
                "of Enfeeblement", "Cripple", "Debilitating", "of the Weak", "Crushing", "Softening",
                "of Fatigue", "Fragility", "of the Damned", "Weighty", "Breaking", "Sapping"
        });
        SHAPE_BUCKETS.put("BOUNCE", new String[]{
                "of Ricochet", "Rebound", "Reflecting", "Echo", "Chain-Reaction", "Deflecting",
                "Elastic", "Resonating", "Orbital", "Flicker", "Spring", "Recurring"
        });
        SHAPE_BUCKETS.put("CHAIN", new String[]{
                "of Connection", "Linking", "Tether", "Shackles", "of the Hive", "Spread",
                "Bonding", "of Unity", "Multicast", "Entwined", "of the Web", "Swarm"
        });
        SHAPE_BUCKETS.put("VAMPIRE", new String[]{
                "of the Night", "Sanguine", "of Blood", "Thirsty", "Vampiric", "of the Bat",
                "Carnage", "Red", "Soul-Eating", "Ghoulish", "Undead", "of the Crypt"
        });
        SHAPE_BUCKETS.put("GRAVITY", new String[]{
                "of the Abyss", "Singularity", "Heavy", "Massive", "Attracting", "of the Black Hole",
                "Absolute", "Newtonian", "Orbiting", "Collapsing", "Crushing", "Weightless"
        });
        SHAPE_BUCKETS.put("WITHER", new String[]{
                "of Decay", "Rotting", "of the Grave", "Despair", "Corrupting", "of Ash",
                "Fading", "Miasmic", "of the Reaper", "Withering", "Death-Touch", "Dread"
        });
        SHAPE_BUCKETS.put("EXECUTE", new String[]{
                "of the Executioner", "Fatal", "Deadly", "Ending", "Final", "Ultimate",
                "of the Guillotine", "Mercy-Kill", "Slaying", "of Judgment", "Termination"
        });
        SHAPE_BUCKETS.put("NONE", new String[]{
                "Art", "Skill", "Technique", "Style", "Move", "Form", "Basics", "Discipline",
                "Method", "Way", "Legacy", "Flow", "Manifestation"
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