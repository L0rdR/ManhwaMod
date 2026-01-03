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

    // --- MILESTONE LOGIC (Every 50 Mana) ---
    public static void saveMana(Player player, int val) {
        player.getPersistentData().putInt(MANA_KEY, val);

        // Automatic unlocks based on your 50-point requirement
        if (val >= 50) checkAndUnlock(player, 1, "Mana Blast", "§bRARE");
        if (val >= 100) checkAndUnlock(player, 2, "Mana Shield", "§6EPIC");
        if (val >= 150) checkAndUnlock(player, 3, "Mana Flight", "§cMYTHIC");

        sync(player);
    }

    private static void checkAndUnlock(Player player, int id, String name, String rarity) {
        String bank = player.getPersistentData().getString(BANK_KEY);
        if (!bank.contains("[" + id + "]")) {
            unlockSkill(player, id, name + ":" + rarity + ":Auto-unlocked at " + (id * 50) + " Mana.", 0);
        }
    }

    // --- REQUIRED BY CLIENTEVENTS / COMMANDS ---
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
    public static int getMana(Player player) {
        int val = player.getPersistentData().getInt(MANA_KEY);
        return val <= 0 ? 10 : val;
    }

    public static int getStrength(Player player) {
        int val = player.getPersistentData().getInt(STR_KEY);
        return val <= 0 ? 10 : val;
    }

    public static int getPoints(Player player) { return player.getPersistentData().getInt(POINTS_KEY); }
    public static void savePoints(Player player, int amount) { player.getPersistentData().putInt(POINTS_KEY, amount); sync(player); }

    public static int getHealthStat(Player player) {
        int val = player.getPersistentData().getInt(HP_KEY);
        return val <= 0 ? 10 : val;
    }

    public static int getDefense(Player player) {
        int val = player.getPersistentData().getInt(DEF_KEY);
        return val <= 0 ? 10 : val;
    }

    public static int getSpeed(Player player) {
        int val = player.getPersistentData().getInt(SPD_KEY);
        return val <= 0 ? 10 : val;
    }

    public static int getCurrentMana(Player player) {
        return player.getPersistentData().getInt(CURRENT_MANA_KEY);
    }

    public static void saveCurrentMana(Player player, int val) {
        player.getPersistentData().putInt(CURRENT_MANA_KEY, val);
        sync(player);
    }

    // --- SYNC & BANK ---
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag data = player.getPersistentData();
            Messages.sendToPlayer(new PacketSyncSystemData(
                    isAwakened(player),
                    getPoints(player),
                    getStrength(player),
                    getHealthStat(player),
                    getDefense(player),
                    getSpeed(player),
                    getMana(player),
                    getCurrentMana(player),
                    isSystemPlayer(player),
                    data.getInt("manhwamod.level"),
                    data.getInt("manhwamod.xp"),
                    data.getString(BANK_KEY) // This is the generated skill list
            ), serverPlayer);
        }
    }

    public static void unlockSkill(Player player, int skillId, String recipeData, int cost) {
        CompoundTag data = player.getPersistentData();
        data.putString("manhwamod.skill_recipe_" + skillId, recipeData);
        data.putInt("manhwamod.skill_cost_" + skillId, cost);
        String bank = data.getString(BANK_KEY);
        if (!bank.contains("[" + skillId + "]")) {
            bank += "[" + skillId + "]";
            data.putString(BANK_KEY, bank);
        }
        sync(player);
    }

    public static List<Integer> getUnlockedSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        String bank = player.getPersistentData().getString(BANK_KEY);
        if (bank.isEmpty()) return list;
        String[] parts = bank.replace("[", "").split("]");
        for (String s : parts) {
            if (!s.isEmpty()) {
                try { list.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }
}