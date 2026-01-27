package com.TaylorBros.ManhwaMod;

public class SkillTags {

    // These determine the movement and particle pattern
    public enum Shape {
        PUNCH, DASH,                      // Tier 1: Basics
        SLASH, VERT_SLASH, HORIZ_SLASH,   // Tier 2: Melee Arts
        SINGLE, BEAM, BALL, RAY,          // Tier 3: Projectiles
        CONE, IMPACT_BURST, FLARE,        // Tier 4: Explosives/AOE
        BOOMERANG, WALL, SPIKES,          // Tier 5: Utility/Zone
        BLINK_STRIKE,                     // Tier 6: High Speed
        BARRAGE, BARRAGE_PUNCH,           // Tier 7: Ultimates
        SLASH_BARRAGE, RAIN, AOE          // Tier 7: Ultimates
    }
  
    // These determine the base particle and main sound
    public enum Element {
        FIRE,
        ICE,
        LIGHTNING,
        VOID,
        FORCE,
        WATER,
        EARTH,
        LAVA,
        LIGHT,
        WIND,
        SHADOW,
        ACID,
        POISON,
        NONE,
    }

    // These determine the special status effect (Potion effects/Explosions)
    public enum Modifier {
        EXPLODE,
        STUN,
        LIFESTEAL,
        WEAKEN,
        BOUNCE,
        CHAIN,
        VAMPIRE,
        GRAVITY,
        WITHER,
        EXECUTE,
        NONE
    }
}