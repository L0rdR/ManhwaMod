package com.TaylorBros.ManhwaMod;

import net.minecraft.ChatFormatting;
import java.util.Arrays;
import java.util.List;

public enum Affinity {
    NONE("None", ChatFormatting.GRAY),
    FIRE("Fire", ChatFormatting.RED),
    ICE("Ice", ChatFormatting.AQUA),
    LIGHTNING("Lightning", ChatFormatting.YELLOW),
    VOID("Void", ChatFormatting.DARK_PURPLE),
    FORCE("Force", ChatFormatting.BOLD),
    WATER("Water", ChatFormatting.BLUE),
    EARTH("Earth", ChatFormatting.GOLD),
    LAVA("Lava", ChatFormatting.DARK_RED),
    LIGHT("Light", ChatFormatting.WHITE),
    WIND("Wind", ChatFormatting.GREEN),
    SHADOW("Shadow", ChatFormatting.BLACK),
    ACID("Acid", ChatFormatting.DARK_GREEN),
    POISON("Poison", ChatFormatting.LIGHT_PURPLE);

    public final String name;
    public final ChatFormatting color;

    Affinity(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    /**
     * Logic: This list defines which elements this specific affinity is STRONG against.
     * When you attack these elements, you deal 10% MORE damage.
     */
    public List<Affinity> getStrengths() {
        return switch (this) {
            case FIRE -> Arrays.asList(ICE, WIND);
            case ICE -> Arrays.asList(WATER, POISON);
            case LIGHTNING -> Arrays.asList(WATER, FORCE);
            case VOID -> Arrays.asList(LIGHT, SHADOW, FORCE);
            case FORCE -> Arrays.asList(EARTH, ACID);
            case WATER -> Arrays.asList(FIRE, LAVA);
            case EARTH -> Arrays.asList(LIGHTNING, POISON);
            case LAVA -> Arrays.asList(ICE, EARTH);
            case LIGHT -> Arrays.asList(VOID, SHADOW);
            case WIND -> Arrays.asList(EARTH, ACID);
            case SHADOW -> Arrays.asList(LIGHT, FORCE);
            case ACID -> Arrays.asList(LAVA, FIRE);
            case POISON -> Arrays.asList(WIND, LIGHT);
            default -> Arrays.asList(); // NONE has no strengths
        };
    }

    /**
     * Helper method to check if this element is weak against another.
     */
    public boolean isWeakTo(Affinity attacker) {
        return attacker.getStrengths().contains(this);
    }
}
