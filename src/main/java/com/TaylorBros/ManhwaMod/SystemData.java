package com.TaylorBros.ManhwaMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;


public class SystemData {
    public static final String POINTS = "manhwamod.points";
    public static final String AWAKENED = "manhwamod.awakened";
    public static final String IS_SYSTEM = "manhwamod.is_system_player";
    public static final String LEVEL = "manhwamod.level";
    public static final String XP = "manhwamod.xp";
    public static final String BANK = "manhwamod.unlocked_skills";

    // THE FIX: Centralized Keys
    public static final String SLOT_PREFIX = "manhwamod.slot_";
    public static final String STR = "manhwamod.str";
    public static final String HP = "manhwamod.health_stat";
    public static final String DEF = "manhwamod.def";
    public static final String SPD = "manhwamod.spd";
    public static final String MANA = "manhwamod.mana";

    public static final String CURRENT_MANA = "manhwamod.current_mana";
    public static final String RECIPE_PREFIX = "manhwamod.skill_recipe_";
    public static final String COST_PREFIX = "manhwamod.skill_cost_";

    // --- ACCESSORS (Now correctly linked to the keys above) ---
    public static int getStrength(Player player) {
        return player.getPersistentData().getInt(STR);
    }

    public static int getHealthStat(Player player) {
        return player.getPersistentData().getInt(HP);
    }

    public static int getDefense(Player player) {
        return player.getPersistentData().getInt(DEF);
    }

    public static int getSpeed(Player player) {
        return player.getPersistentData().getInt(SPD);
    }

    public static int getMana(Player player) {
        return player.getPersistentData().getInt(MANA);
    }


    public static int getCurrentMana(Player player) {
        return player.getPersistentData().getInt(CURRENT_MANA);
    }

    public static int getPoints(Player player) {
        return player.getPersistentData().getInt(POINTS);
    }

    public static boolean isAwakened(Player player) {
        return player.getPersistentData().getBoolean(AWAKENED);
    }

    public static boolean isSystemPlayer(Player player) {
        return player.getPersistentData().getBoolean(IS_SYSTEM);
    }

    // --- MUTATORS ---
    public static void savePoints(Player player, int val) {
        player.getPersistentData().putInt(POINTS, val);
        sync(player);
    }

    public static void saveCurrentMana(Player player, int val) {
        player.getPersistentData().putInt(CURRENT_MANA, val);
        sync(player);
    }

    public static void saveAwakening(Player player, boolean val) {
        player.getPersistentData().putBoolean(AWAKENED, val);
        sync(player);
    }

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
        for (String s : parts) {
            if (!s.isEmpty()) try {
                list.add(Integer.parseInt(s.trim()));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    // --- SYNCING ---
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag nbt = player.getPersistentData();

            // Wrap the data we want to send
            CompoundTag syncData = new CompoundTag();
            syncData.putBoolean("manhwamod.is_system_player", nbt.getBoolean("manhwamod.is_system_player"));

            // Sync the slots so the HUD knows what to display
            for (int i = 0; i < 5; i++) {
                String key = "manhwamod.slot_" + i;
                syncData.putString(key, nbt.getString(key));
            }
            // This tells the Client to update its UI with the new Server data instantly
            Messages.sendToPlayer(new PacketSyncSystemData(player.getPersistentData()), serverPlayer);
        }
    }

    public static String getSkillRecipe(Player player, int skillId) {

        return player.getPersistentData().getString(SLOT_PREFIX + skillId);    }
}