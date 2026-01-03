package com.TaylorBros.ManhwaMod;

import net.minecraft.world.entity.player.Player;
import java.time.LocalDate;

public class DailyQuestData {
    // NBT Keys
    private static final String LAST_RESET = "manhwamod.quest_date";
    private static final String MOB_KILLS = "manhwamod.quest_kills";
    private static final String DISTANCE_RUN = "manhwamod.quest_dist";
    private static final String COMPLETED = "manhwamod.quest_done";

    // Targets
    public static final int KILL_TARGET = 50;
    public static final int DIST_TARGET = 2000; // Blocks

    public static void checkAndReset(Player player) {
        String today = LocalDate.now().toString();
        String lastDate = player.getPersistentData().getString(LAST_RESET);

        if (!today.equals(lastDate)) {
            player.getPersistentData().putString(LAST_RESET, today);
            player.getPersistentData().putInt(MOB_KILLS, 0);
            player.getPersistentData().putDouble(DISTANCE_RUN, 0);
            player.getPersistentData().putBoolean(COMPLETED, false);

            if (player.getPersistentData().getBoolean("manhwamod.is_system_player")) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[SYSTEM] §fDaily Quest has arrived."), false);
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
        checkCompletion(player);
    }

    public static void addDistance(Player player, double amount) {
        if (isCompleted(player)) return;
        double dist = getDist(player) + amount;
        player.getPersistentData().putDouble(DISTANCE_RUN, dist);
        if ((int)dist % 500 == 0) checkCompletion(player); // Only check periodically for performance
    }

    private static void checkCompletion(Player player) {
        if (getKills(player) >= KILL_TARGET && getDist(player) >= DIST_TARGET) {
            player.getPersistentData().putBoolean(COMPLETED, true);
            SystemData.savePoints(player, SystemData.getPoints(player) + 3); // The Reward
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[SYSTEM] §fDaily Quest Completed! +3 Points"), false);
            SystemData.sync(player);
        }
    }
}