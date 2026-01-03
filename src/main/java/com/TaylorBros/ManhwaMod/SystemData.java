package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import java.util.List;
import java.util.ArrayList;

public class SystemData {
    // RULE 1: CENTRALIZED CONSTANTS
    public static final String AWAKENED = "manhwamod.awakened";
    public static final String MANA = "manhwamod.mana";
    public static final String CURRENT_MANA = "manhwamod.current_mana";
    public static final String POINTS = "manhwamod.points";
    public static final String BANK = "manhwamod.unlocked_skills";
    public static final String RECIPE_PREFIX = "manhwamod.skill_recipe_";
    public static final String COST_PREFIX = "manhwamod.skill_cost_";
    public static final String SLOT_PREFIX = "manhwamod.slot_";

    public static void saveMana(Player player, int val) {
        player.getPersistentData().putInt(MANA, val);
        if (val >= 50) checkAndUnlock(player, 1, "Mana Blast", "§bRARE");
        sync(player);
    }

    private static void checkAndUnlock(Player player, int id, String name, String rarity) {
        String bank = player.getPersistentData().getString(BANK);
        if (!bank.contains("[" + id + "]")) {
            unlockSkill((ServerPlayer)player, id, name + ":" + rarity + ":Auto-unlocked", 0);
        }
    }

    public static int getCurrentMana(Player player) { return player.getPersistentData().getInt(CURRENT_MANA); }
    public static void saveCurrentMana(Player player, int val) { player.getPersistentData().putInt(CURRENT_MANA, val); sync(player); }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // RULE: Send the whole folder (NBT) to ensure recipes sync
            Messages.sendToPlayer(new PacketSyncSystemData(player.getPersistentData()), serverPlayer);
        }
    }

    public static void unlockSkill(ServerPlayer player, int id, String recipe, int cost) {
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
        String bank = player.getPersistentData().getString(BANK);
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
        for (int id : list) sb.append("[").append(id).append("]");
        player.getPersistentData().putString(BANK, sb.toString());
    }
}