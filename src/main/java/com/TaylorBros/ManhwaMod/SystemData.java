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
    public static final String STR = "manhwamod.strength";
    public static final String HP = "manhwamod.health_stat";
    public static final String DEF = "manhwamod.defense";
    public static final String SPD = "manhwamod.speed";
    public static final String MANA = "manhwamod.mana"; // Intelligence

    public static final String CURRENT_MANA = "manhwamod.current_mana";
    public static final String RECIPE_PREFIX = "manhwamod.skill_recipe_";
    public static final String COST_PREFIX = "manhwamod.skill_cost_";
    public static final String LAST_USE_PREFIX = "manhwamod.last_use_";
    public static final String COOLDOWN_PREFIX = "manhwamod.cooldown_";

    // --- ACCESSORS ---
    public static int getStrength(Player player) { return player.getPersistentData().getInt(STR); }
    public static int getHealthStat(Player player) { int val = player.getPersistentData().getInt(HP); return val == 0 ? 20 : val; }
    public static int getSpeed(Player player) { return player.getPersistentData().getInt(SPD); }
    public static int getDefense(Player player) { return player.getPersistentData().getInt(DEF); }
    public static int getPoints(Player player) { return player.getPersistentData().getInt(POINTS); }
    public static int getCurrentMana(Player player) { return player.getPersistentData().getInt(CURRENT_MANA); }
    public static int getMana(Player player) { return player.getPersistentData().getInt(MANA); }
    public static int getLevel(Player player) { return player.getPersistentData().getInt(LEVEL); }
    public static int getXP(Player player) { return player.getPersistentData().getInt(XP); }
    public static boolean isSystemPlayer(Player player) { return player.getPersistentData().getBoolean(IS_SYSTEM); }
    public static boolean isAwakened(Player player) { return isSystemPlayer(player) || player.getPersistentData().getBoolean(AWAKENED); }

    public static void savePoints(Player player, int val) { player.getPersistentData().putInt(POINTS, val); sync(player); }
    public static void saveCurrentMana(Player player, int val) { player.getPersistentData().putInt(CURRENT_MANA, val); sync(player); }

    // --- SYNCING (FIXED) ---
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag nbt = player.getPersistentData();
            CompoundTag syncData = new CompoundTag();

            syncData.putString(BANK, nbt.getString(BANK));
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
            syncData.putString("manhwamod.affinity", nbt.getString("manhwamod.affinity"));

            for (int i = 0; i < 5; i++) {
                String key = SLOT_PREFIX + i;
                syncData.putInt(key, nbt.getInt(key));
            }

            // Sync Recipes AND COSTS
            List<Integer> unlocked = getUnlockedSkills(player);
            for (int id : unlocked) {
                String rKey = RECIPE_PREFIX + id;
                syncData.putString(rKey, nbt.getString(rKey));

                // Fixed: Sync the cost!
                String cKey = COST_PREFIX + id;
                syncData.putInt(cKey, nbt.getInt(cKey));
            }

            // Sync Cooldowns
            for (int i = 0; i < 5; i++) {
                String lastUseKey = LAST_USE_PREFIX + i;
                String cooldownKey = COOLDOWN_PREFIX + i;
                syncData.putLong(lastUseKey, nbt.getLong(lastUseKey));
                syncData.putInt(cooldownKey, nbt.getInt(cooldownKey));
            }

            Messages.sendToPlayer(new PacketSyncSystemData(syncData), serverPlayer);
        }
    }

    public static String getSkillRecipe(Player player, int slot) {
        int skillId = player.getPersistentData().getInt(SLOT_PREFIX + slot);
        if (skillId <= 0) return "";

        String rawData = player.getPersistentData().getString(RECIPE_PREFIX + skillId);

        // If it contains the name, only return the RECIPE part for the Skill Engine to use
        if (rawData.contains("|")) {
            return rawData.split("\\|")[0];
        }

        return rawData;
    }

    public static List<Integer> getUnlockedSkills(Player player) {
        List<Integer> list = new ArrayList<>();
        if (player == null) return list;
        String bank = player.getPersistentData().getString(BANK);
        if (bank.isEmpty()) return list;
        String[] parts = bank.replace("[", "").split("]");
        for (String s : parts) {
            if (!s.isEmpty()) try { list.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
        }
        return list;
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

    public static void saveAwakening(Player player, boolean val) { player.getPersistentData().putBoolean(AWAKENED, val); sync(player); }
    public static void saveStrength(Player player, int val) { player.getPersistentData().putInt(STR, val); sync(player); }
    public static void saveHealthStat(Player player, int val) { player.getPersistentData().putInt(HP, val); sync(player); }
    public static void saveDefense(Player player, int val) { player.getPersistentData().putInt(DEF, val); sync(player); }
    public static void saveSpeed(Player player, int val) { player.getPersistentData().putInt(SPD, val); sync(player); }
    public static void saveMana(Player player, int val) { player.getPersistentData().putInt(MANA, val); sync(player); }

    public static void setAffinity(Player player, Affinity affinity) {
        player.getPersistentData().putString("manhwamod.affinity", affinity.name());
        // ADD THIS LINE: Force the update to the client immediately
        sync(player);
    }

    public static Affinity getAffinity(Player player) {
        String name = player.getPersistentData().getString("manhwamod.affinity");
        try {
            return name.isEmpty() ? Affinity.NONE : Affinity.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Affinity.NONE;
        }
    }
}