package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SystemData {
    // RULE 1: CENTRALIZED CONSTANTS
    public static final String POINTS = "manhwamod.points";
    public static final String AWAKENED = "manhwamod.awakened";
    public static final String IS_SYSTEM = "manhwamod.is_system_player";
    public static final String MANA = "manhwamod.mana";
    public static final String CURRENT_MANA = "manhwamod.current_mana";
    public static final String LEVEL = "manhwamod.level";
    public static final String XP = "manhwamod.xp";

    // CASCADE FIX: Added missing prefixes required by AwakenedStatusScreen
    public static final String SLOT_PREFIX = "manhwamod.slot_";
    public static final String RECIPE_PREFIX = "manhwamod.skill_recipe_";
    public static final String COST_PREFIX = "manhwamod.skill_cost_";

    // --- HELPER METHODS ---
    public static boolean isAwakened(Player player) { return player.getPersistentData().getBoolean(AWAKENED); }
    public static boolean isSystemPlayer(Player player) { return player.getPersistentData().getBoolean(IS_SYSTEM); }
    public static int getPoints(Player player) { return player.getPersistentData().getInt(POINTS); }

    public static void savePoints(Player player, int val) {
        player.getPersistentData().putInt(POINTS, val);
        sync(player);
    }

    public static int getMana(Player player) { return player.getPersistentData().getInt(MANA); }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Messages.sendToPlayer(new PacketSyncSystemData(player.getPersistentData()), serverPlayer);
        }
    }
}