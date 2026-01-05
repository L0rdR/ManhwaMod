package com.TaylorBros.ManhwaMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDate;

public class DailyQuestData {
    private static final String LAST_RESET = "manhwamod.quest_date";
    private static final String MOB_KILLS = "manhwamod.quest_kills";
    private static final String DISTANCE_RUN = "manhwamod.quest_dist";
    private static final String COMPLETED = "manhwamod.quest_done";

    // Dynamic Target Calculations
    public static int getKillTarget(Player player) {
        int level = player.getPersistentData().getInt("manhwamod.level");
        // Base 50 + 5 per level (e.g., Level 10 = 100 kills)
        return 50 + (level * 5);
    }

    public static int getDistTarget(Player player) {
        int level = player.getPersistentData().getInt("manhwamod.level");
        // Base 2000 + 200 per level (e.g., Level 10 = 4000 blocks)
        return 2000 + (level * 200);
    }

    public static void checkAndReset(Player player) {
        String today = LocalDate.now().toString();
        String lastDate = player.getPersistentData().getString(LAST_RESET);

        // FIX: Define the missing 'isSystem' variable
        boolean isSystem = player.getPersistentData().getBoolean("manhwamod.is_system_player");

        // Reset if it's a new day OR if the player is a System Player but has no date recorded (Wiped)
        if (!today.equals(lastDate) || (isSystem && lastDate.isEmpty())) {
            player.getPersistentData().putString(LAST_RESET, today);
            player.getPersistentData().putInt(MOB_KILLS, 0);
            player.getPersistentData().putDouble(DISTANCE_RUN, 0);
            player.getPersistentData().putBoolean(COMPLETED, false);

            if (isSystem) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[SYSTEM] §fDaily Quest has arrived."), false);
                // Inform the player of their current targets
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7Goal: " + getKillTarget(player) + " Kills | " + getDistTarget(player) + "m Run"), false);
            }

            // Immediately sync the reset to the client so the UI updates
            if (player instanceof ServerPlayer serverPlayer) {
                SystemData.sync(serverPlayer);
            }
        }
    }

    public static boolean isCompleted(Player player) { return player.getPersistentData().getBoolean(COMPLETED); }
    public static int getKills(Player player) { return player.getPersistentData().getInt(MOB_KILLS); }
    public static double getDist(Player player) { return player.getPersistentData().getDouble(DISTANCE_RUN); }

    public static void addKill(Player player) {
        if (isCompleted(player)) return;
        int kills = getKills(player) + 1;
        player.getPersistentData().putInt(MOB_KILLS, kills);

        if (player instanceof ServerPlayer serverPlayer) {
            SystemData.sync(serverPlayer);
        }
        checkCompletion(player);
    }

    public static void addDistance(Player player, double amount) {
        if (isCompleted(player)) return;
        double dist = getDist(player) + amount;
        player.getPersistentData().putDouble(DISTANCE_RUN, dist);

        if (dist % 10 < amount && player instanceof ServerPlayer serverPlayer) {
            SystemData.sync(serverPlayer);
        }
        if ((int)dist % 100 == 0) checkCompletion(player);
    }

    private static void checkCompletion(Player player) {
        if (getKills(player) >= getKillTarget(player) && getDist(player) >= getDistTarget(player)) {
            player.getPersistentData().putBoolean(COMPLETED, true);
            SystemData.savePoints(player, SystemData.getPoints(player) + 3);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[SYSTEM] §fDaily Quest Completed! +3 Points"), false);
            SystemData.sync(player);
        }
    }
}