package com.TaylorBros.ManhwaMod;

public class SkillTags {
    public enum Shape {
        PUNCH, DASH,
        SLASH, VERT_SLASH, HORIZ_SLASH,
        SINGLE, BEAM, BALL, RAY,
        CONE, IMPACT_BURST, FLARE,
        BOOMERANG, WALL, SPIKES,
        BLINK_STRIKE,
        BARRAGE, BARRAGE_PUNCH,
        SLASH_BARRAGE, RAIN, AOE,

        // --- NEW VISUAL SHAPES ---
        SMOKE, SPARK, BOLT, CIRCLE, STAR
    }

    public enum Element {
        FIRE, ICE, LIGHTNING, VOID, FORCE, WATER, EARTH,
        LAVA, LIGHT, WIND, SHADOW, ACID, POISON, NONE
    }

    public enum Modifier {
        EXPLODE, STUN, LIFESTEAL, WEAKEN, BOUNCE, CHAIN,
        VAMPIRE, GRAVITY, WITHER, EXECUTE, HOMING, PIERCE,
        BLIND, SLOW, NONE
    }
}