package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import java.util.List;
import java.util.ArrayList;

public class SystemData {
    // CENTRALIZED KEYS - Rule 1
    public static final String AWAKENED = "manhwamod.awakened";
    public static final String POINTS = "manhwamod.points";
    public static final String MANA = "manhwamod.mana";
    public static final String CURRENT_MANA = "manhwamod.current_mana";
    public static final String BANK = "manhwamod.unlocked_skills";
    public static final String LEVEL = "manhwamod.level";
    public static final String XP = "manhwamod.xp";
    public static final String IS_SYSTEM = "manhwamod.is_system_player";

    // Skill Specific Keys
    public static final String SLOT_PREFIX = "manhwamod.slot_";
    public static final String RECIPE_PREFIX = "manhwamod.skill_recipe_";
    public static final String COST_PREFIX = "manhwamod.skill_cost_";

    public static void saveMana(Player player, int val) {
        player.getPersistentData().putInt(MANA, val);
        sync(player);
    }

    public static boolean isAwakened(Player player) {
        return player.getPersistentData().getBoolean(AWAKENED);
    }

    public static void saveAwakening(Player player, boolean value) {
        player.getPersistentData().putBoolean(AWAKENED, value);
        sync(player);
    }

    public static int getMana(Player player) { return player.getPersistentData().getInt(MANA); }
    public static int getPoints(Player player) { return player.getPersistentData().getInt(POINTS); }
    public static int getCurrentMana(Player player) { return player.getPersistentData().getInt(CURRENT_MANA); }

    public static void saveCurrentMana(Player player, int val) {
        player.getPersistentData().putInt(CURRENT_MANA, val);
        sync(player);
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Messages.sendToPlayer(new PacketSyncSystemData(player.getPersistentData()), serverPlayer);
        }
    }

    public static void unlockSkill(Player player, int id, String recipe, int cost) {
        List<Integer> unlocked = getUnlockedSkills(player);
        if (!unlocked.contains(id)) {
            unlocked.add(id);
            saveUnlockedSkills(player, unlocked);
            player.getPersistentData().putString(RECIPE_PREFIX + id, recipe);
            player.getPersistentData().putInt(COST_PREFIX + id, cost);
            sync(player);
        }
    }

    public static List<Integer> getUnlockedSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        String bankStr = player.getPersistentData().getString(BANK);
        if (bankStr.isEmpty()) return list;
        String[] parts = bankStr.replace("[", "").split("]");
        for (String s : parts) {
            if (!s.isEmpty()) {
                try { list.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    public static void saveUnlockedSkills(Player player, List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int id : list) sb.append("[").append(id).append("]");
        player.getPersistentData().putString(BANK, sb.toString());
    }
}