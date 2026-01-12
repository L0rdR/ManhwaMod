package com.TaylorBros.ManhwaMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.ArrayList;

public class SystemData {
    public static final String POINTS = "manhwamod.points";
    public static final String AWAKENED = "manhwamod.awakened";
    public static final String IS_SYSTEM = "manhwamod.is_system_player";
    public static final String LEVEL = "manhwamod.level";
    public static final String XP = "manhwamod.xp";
    public static final String BANK = "manhwamod.unlocked_skills";


    public static final String SLOT_PREFIX = "manhwamod.slot_";
    // These are the DEFINITIVE keys. Do not change them again.
    public static final String STR = "manhwamod.strength";
    public static final String HP = "manhwamod.health_stat";
    public static final String DEF = "manhwamod.defense";
    public static final String SPD = "manhwamod.speed";
    public static final String MANA = "manhwamod.mana";
    //
    public static final String CURRENT_MANA = "manhwamod.current_mana";
    public static final String RECIPE_PREFIX = "manhwamod.skill_recipe_";
    public static final String COST_PREFIX = "manhwamod.skill_cost_";
    public static final String LAST_USE_PREFIX = "manhwamod.last_use_";
    public static final String COOLDOWN_PREFIX = "manhwamod.cooldown_";
    // --- ACCESSORS ---
    public static int getStrength(Player player) {
        return player.getPersistentData().getInt(STR);
    }

    public static int getHealthStat(Player player) {
        int val = player.getPersistentData().getInt(HP);
        return val == 0 ? 20 : val; // If data is wiped, default to 20 (10 hearts)
    }
    public static int getSpeed(Player player) { return player.getPersistentData().getInt(SPD); }

    public static int getDefense(Player player) {
        return player.getPersistentData().getInt(DEF);
    }

    public static int getPoints(Player player) {
        return player.getPersistentData().getInt(POINTS);
    }
    public static int getCurrentMana(Player player) { return player.getPersistentData().getInt(CURRENT_MANA); }

    public static int getMana(Player player) { return player.getPersistentData().getInt(MANA); }

    public static int getLevel(Player player) { return player.getPersistentData().getInt(LEVEL); }
    public static int getXP(Player player) { return player.getPersistentData().getInt(XP); }

    public static boolean isSystemPlayer(Player player) {
        return player.getPersistentData().getBoolean(IS_SYSTEM);
    }
    public static boolean isAwakened(Player player) {
        return isSystemPlayer(player)
                || player.getPersistentData().getBoolean(AWAKENED);
    }
    public static void savePoints(Player player, int val) {
        player.getPersistentData().putInt(POINTS, val);
        sync(player);
    }

    // --- SYNCING ---
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag nbt = player.getPersistentData();
            CompoundTag syncData = new CompoundTag();

            // 1. Sync the BANK (This restores your Scroll)
            syncData.putString(BANK, nbt.getString(BANK));

            // 2. Sync the STATS (This restores your Status Screen)
            syncData.putInt(STR, nbt.getInt(STR));
            syncData.putInt(HP, nbt.getInt(HP));
            syncData.putInt(DEF, nbt.getInt(DEF));
            syncData.putInt(MANA, nbt.getInt(MANA));
            syncData.putInt(SPD, nbt.getInt(SPD));
            syncData.putInt(POINTS, nbt.getInt(POINTS));
            syncData.putBoolean(AWAKENED, nbt.getBoolean(AWAKENED));
            syncData.putBoolean(IS_SYSTEM, nbt.getBoolean(IS_SYSTEM));
            syncData.putInt(LEVEL, nbt.getInt(LEVEL));
            syncData.putInt(CURRENT_MANA, nbt.getInt(CURRENT_MANA));
            syncData.putInt(XP, nbt.getInt(XP));

            for (int i = 0; i < 5; i++) {
                String key = SLOT_PREFIX + i;
                syncData.putInt(key, nbt.getInt(key));
            }

            // 4. Sync Recipes for the Screen
            List<Integer> unlocked = getUnlockedSkills(player);
            for (int id : unlocked) {
                String rKey = RECIPE_PREFIX + id;
                syncData.putString(rKey, nbt.getString(rKey));
            }
            // 5. Sync Cooldowns for Equipped Skills (Required for HUD)
            for (int i = 0; i < 5; i++) {
                int id = nbt.getInt(SLOT_PREFIX + i);
                if (id != 0) {
                    String unlockKey = "manhwamod.cd_timer_" + id;
                    String durKey = "manhwamod.cd_duration_" + id;
                    syncData.putLong(unlockKey, nbt.getLong(unlockKey));
                    syncData.putInt(durKey, nbt.getInt(durKey));
                }
            }

            Messages.sendToPlayer(new PacketSyncSystemData(syncData), serverPlayer);
        }
    }

    public static List<Integer> getUnlockedSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        if (player == null) return list;

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

    public static String getSkillRecipe(Player player, int slotIndex) {
        int skillId = player.getPersistentData().getInt(SLOT_PREFIX + slotIndex);
        if (skillId == 0) return "";
        return player.getPersistentData().getString(RECIPE_PREFIX + skillId);
    }
    public static void saveCurrentMana(Player player, int val) {
        player.getPersistentData().putInt(CURRENT_MANA, val);
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
    public static void saveAwakening(Player player, boolean val) {
        player.getPersistentData().putBoolean(AWAKENED, val);
        sync(player); // This must be here to update the client
    }
    public static void saveStrength(Player player, int val) {
        player.getPersistentData().putInt(STR, val);
        sync(player);
    }

    public static void saveHealthStat(Player player, int val) {
        player.getPersistentData().putInt(HP, val);
        sync(player);
    }

    public static void saveDefense(Player player, int val) {
        player.getPersistentData().putInt(DEF, val);
        sync(player);
    }

    public static void saveSpeed(Player player, int val) {
        player.getPersistentData().putInt(SPD, val);
        sync(player);
    }

    public static void saveMana(Player player, int val) {
        player.getPersistentData().putInt(MANA, val);
        sync(player);
    }



}