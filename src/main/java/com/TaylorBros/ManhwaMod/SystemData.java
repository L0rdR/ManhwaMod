package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SystemData {
    private static final String AWAKENED_KEY = "manhwamod.awakened";
    private static final String STR_KEY = "manhwamod.strength";
    private static final String POINTS_KEY = "manhwamod.points";
    private static final String HP_KEY = "manhwamod.health_stat";
    private static final String DEF_KEY = "manhwamod.defense";
    private static final String SPD_KEY = "manhwamod.speed";
    private static final String MANA_KEY = "manhwamod.mana";
    private static final String CURRENT_MANA_KEY = "manhwamod.current_mana";

    // --- RANK LOGIC (Scale 1-1000) ---
    public static String getRank(Player player) {
        int level = player.getPersistentData().getInt("manhwamod.level");
        if (level >= 900) return "§c§lNATIONAL";
        if (level >= 750) return "§6§lS";
        if (level >= 500) return "§e§lA";
        if (level >= 300) return "§d§lB";
        if (level >= 150) return "§b§lC";
        if (level >= 50)  return "§a§lD";
        return "§7§lE";
    }

    public static boolean isAwakened(Player player) { return player.getPersistentData().getBoolean(AWAKENED_KEY); }
    public static void saveAwakening(Player player, boolean value) { player.getPersistentData().putBoolean(AWAKENED_KEY, value); sync(player); }

    // --- STAT HELPERS WITH DEFAULTS (Prevents Freezing) ---
    public static int getStrength(Player player) { return player.getPersistentData().contains(STR_KEY) ? player.getPersistentData().getInt(STR_KEY) : 10; }
    public static void saveStrength(Player player, int amount) { player.getPersistentData().putInt(STR_KEY, amount); sync(player); }

    public static int getPoints(Player player) { return player.getPersistentData().contains(POINTS_KEY) ? player.getPersistentData().getInt(POINTS_KEY) : 5; }
    public static void savePoints(Player player, int amount) { player.getPersistentData().putInt(POINTS_KEY, amount); sync(player); }

    public static int getHealthStat(Player player) { return player.getPersistentData().contains(HP_KEY) ? player.getPersistentData().getInt(HP_KEY) : 10; }
    public static void saveHealthStat(Player player, int val) { player.getPersistentData().putInt(HP_KEY, val); sync(player); }

    public static int getDefense(Player player) { return player.getPersistentData().contains(DEF_KEY) ? player.getPersistentData().getInt(DEF_KEY) : 10; }
    public static void saveDefense(Player player, int val) { player.getPersistentData().putInt(DEF_KEY, val); sync(player); }

    public static int getSpeed(Player player) { return player.getPersistentData().contains(SPD_KEY) ? player.getPersistentData().getInt(SPD_KEY) : 10; }
    public static void saveSpeed(Player player, int val) { player.getPersistentData().putInt(SPD_KEY, val); sync(player); }

    public static int getMana(Player player) { return player.getPersistentData().contains(MANA_KEY) ? player.getPersistentData().getInt(MANA_KEY) : 10; }
    public static void saveMana(Player player, int val) { player.getPersistentData().putInt(MANA_KEY, val); sync(player); }

    public static int getCurrentMana(Player player) { return player.getPersistentData().getInt(CURRENT_MANA_KEY); }
    public static void saveCurrentMana(Player player, int val) { player.getPersistentData().putInt(CURRENT_MANA_KEY, val); sync(player); }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Messages.sendToPlayer(new PacketSyncSystemData(
                    isAwakened(player),
                    getPoints(player),
                    getStrength(player),
                    getHealthStat(player),
                    getDefense(player),
                    getSpeed(player),
                    getMana(player),
                    getCurrentMana(player),
                    player.getPersistentData().getBoolean("manhwamod.is_system_player"),
                    player.getPersistentData().getInt("manhwamod.level"), // ADDED
                    player.getPersistentData().getInt("manhwamod.xp")    // ADDED
            ), serverPlayer);
        }
    }
}