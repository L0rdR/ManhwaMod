package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import java.util.List;
import java.util.ArrayList;

public class SystemData {
    private static final String AWAKENED_KEY = "manhwamod.awakened";
    private static final String STR_KEY = "manhwamod.strength";
    private static final String POINTS_KEY = "manhwamod.points";
    private static final String HP_KEY = "manhwamod.health_stat";
    private static final String DEF_KEY = "manhwamod.defense";
    private static final String SPD_KEY = "manhwamod.speed";
    private static final String MANA_KEY = "manhwamod.mana";
    private static final String CURRENT_MANA_KEY = "manhwamod.current_mana";
    private static final String BANK_KEY = "manhwamod.unlocked_skills";

    // --- MILESTONE LOGIC ---
    public static void saveMana(Player player, int val) {
        player.getPersistentData().putInt(MANA_KEY, val);
        // Ensure milestones trigger manifestation
        sync(player);
    }

    public static boolean isAwakened(Player player) {
        return player.getPersistentData().getBoolean(AWAKENED_KEY);
    }

    public static void saveAwakening(Player player, boolean value) {
        player.getPersistentData().putBoolean(AWAKENED_KEY, value);
        sync(player);
    }

    public static boolean isSystemPlayer(Player player) {
        return player.getPersistentData().getBoolean("manhwamod.is_system_player");
    }

    // --- STAT HELPERS ---
    public static int getMana(Player player) { return player.getPersistentData().getInt(MANA_KEY); }
    public static int getStrength(Player player) { return player.getPersistentData().getInt(STR_KEY); }
    public static int getPoints(Player player) { return player.getPersistentData().getInt(POINTS_KEY); }
    public static void savePoints(Player player, int amount) { player.getPersistentData().putInt(POINTS_KEY, amount); sync(player); }
    public static int getHealthStat(Player player) { return player.getPersistentData().getInt(HP_KEY); }
    public static int getDefense(Player player) { return player.getPersistentData().getInt(DEF_KEY); }
    public static int getSpeed(Player player) { return player.getPersistentData().getInt(SPD_KEY); }
    public static int getCurrentMana(Player player) { return player.getPersistentData().getInt(CURRENT_MANA_KEY); }

    public static void saveCurrentMana(Player player, int val) {
        player.getPersistentData().putInt(CURRENT_MANA_KEY, val);
        sync(player);
    }

    // --- SYNC & BANK ---
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag data = player.getPersistentData();
            Messages.sendToPlayer(new PacketSyncSystemData(player.getPersistentData()), serverPlayer);
        }
    }

    public static void unlockSkill(Player player, int id, String recipe, int cost) {
        List<Integer> unlocked = getUnlockedSkills(player);
        if (!unlocked.contains(id)) {
            unlocked.add(id);
            saveUnlockedSkills(player, unlocked); // Fixed: This was missing logic

            // Permanent NBT Storage
            player.getPersistentData().putString("manhwamod.skill_recipe_" + id, recipe);
            player.getPersistentData().putInt("manhwamod.skill_cost_" + id, cost);

            sync(player);
        }
    }

    public static List<Integer> getUnlockedSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        String bank = player.getPersistentData().getString(BANK_KEY);
        if (bank.isEmpty()) return list;
        String[] parts = bank.replace("[", "").split("]");
        for (String s : parts) {
            if (!s.isEmpty()) {
                try { list.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    public static void saveUnlockedSkills(Player player, List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int id : list) {
            sb.append("[").append(id).append("]");
        }
        player.getPersistentData().putString(BANK_KEY, sb.toString());
    }
}