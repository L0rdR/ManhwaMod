package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.ArrayList;

public class SystemData {
    // RULE 1: CENTRALIZED CONSTANTS
    public static final String POINTS = "manhwamod.points";
    public static final String AWAKENED = "manhwamod.awakened";
    public static final String IS_SYSTEM = "manhwamod.is_system_player";
    public static final String MANA = "manhwamod.mana";
    public static final String CURRENT_MANA = "manhwamod.current_mana";
    public static final String LEVEL = "manhwamod.level";
    public static final String XP = "manhwamod.xp";
    public static final String BANK = "manhwamod.unlocked_skills";
    public static final String SLOT_PREFIX = "manhwamod.slot_";
    public static final String RECIPE_PREFIX = "manhwamod.skill_recipe_";
    public static final String COST_PREFIX = "manhwamod.skill_cost_";

    // --- ACCESSORS ---
    public static boolean isAwakened(Player player) { return player.getPersistentData().getBoolean(AWAKENED); }
    public static boolean isSystemPlayer(Player player) { return player.getPersistentData().getBoolean(IS_SYSTEM); }
    public static int getPoints(Player player) { return player.getPersistentData().getInt(POINTS); }
    public static int getMana(Player player) { return player.getPersistentData().getInt(MANA); }
    public static int getCurrentMana(Player player) { return player.getPersistentData().getInt(CURRENT_MANA); }


    public static int getStrength(Player player) { return player.getPersistentData().getInt("manhwamod.strength"); }
    public static int getHealthStat(Player player) { return player.getPersistentData().getInt("manhwamod.health"); }
    public static int getDefense(Player player) { return player.getPersistentData().getInt("manhwamod.defense"); }
    public static int getSpeed(Player player) { return player.getPersistentData().getInt("manhwamod.speed"); }

    // --- MUTATORS ---
    public static void savePoints(Player player, int val) { player.getPersistentData().putInt(POINTS, val); sync(player); }
    public static void saveCurrentMana(Player player, int val) {
        player.getPersistentData().putInt(CURRENT_MANA, val);
        sync(player);
    }
    public static void saveAwakening(Player player, boolean val) {
        player.getPersistentData().putBoolean(AWAKENED, val);
        sync(player);
    }

    // FIXED: Added the missing unlockSkill method
    public static void unlockSkill(Player player, int id, String recipe, int cost) {
        List<Integer> unlocked = getUnlockedSkills(player);
        if (!unlocked.contains(id)) {
            String currentBank = player.getPersistentData().getString(BANK);
            player.getPersistentData().putString(BANK, currentBank + "[" + id + "]");
        }
        player.getPersistentData().putString(RECIPE_PREFIX + id, recipe);
        player.getPersistentData().putInt(COST_PREFIX + id, cost);
        sync(player);
    }

    public static List<Integer> getUnlockedSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        String bank = player.getPersistentData().getString(BANK);
        if (bank.isEmpty()) return list;
        String[] parts = bank.replace("[", "").split("]");
        for (String s : parts) { if (!s.isEmpty()) try { list.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {} }
        return list;
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Messages.sendToPlayer(new PacketSyncSystemData(player.getPersistentData()), serverPlayer);
        }
    }
}