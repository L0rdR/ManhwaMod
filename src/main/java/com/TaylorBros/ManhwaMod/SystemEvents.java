package com.TaylorBros.ManhwaMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ManhwaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SystemEvents {

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int currentMana = player.getPersistentData().getInt(SystemData.CURRENT_MANA);
            player.getPersistentData().putInt(SystemData.CURRENT_MANA, currentMana);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            DailyQuestData.addKill(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        // Handled by Minecraft; no sync needed here.
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DailyQuestData.checkAndReset(player);
            // Ensure default stats exist for new players
            if (!player.getPersistentData().contains("manhwamod.points")) {
                player.getPersistentData().putInt("manhwamod.points", 5);
                player.getPersistentData().putInt("manhwamod.strength", 10);
                player.getPersistentData().putInt("manhwamod.health_stat", 10);
                player.getPersistentData().putInt(SystemData.MANA, 10);
                player.getPersistentData().putInt(SystemData.CURRENT_MANA, 100);
                player.getPersistentData().putInt("manhwamod.speed", 10);
                player.getPersistentData().putInt("manhwamod.defense", 10);
                player.getPersistentData().putBoolean("manhwamod.awakened", false);
            }
                // FORCE QUEST RESET ON NEW PLAYER / WIPE
                if (player.getPersistentData().getBoolean("manhwamod.is_system_player")) {
                    // If the player has no quest date recorded, they are likely fresh/wiped
                    if (player.getPersistentData().getString("manhwamod.quest_date").isEmpty()) {
                        DailyQuestData.checkAndReset(player);
                    }
                }
            SystemData.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;

            // 1. Mana Regeneration (Server-side)
            if (player instanceof ServerPlayer sPlayer) {
                int manaStat = SystemData.getMana(sPlayer);
                int maxCap = manaStat * 10;
                int currentMana = SystemData.getCurrentMana(sPlayer);

                if (currentMana < maxCap) {
                    double regenPerTick = 0.05 + (manaStat / 200.0);
                    double buffer = sPlayer.getPersistentData().getDouble("manhwamod.mana_regen_buffer");
                    double totalAddition = regenPerTick + buffer;

                    int toAdd = (int) totalAddition;
                    double newBuffer = totalAddition - toAdd;

                    if (toAdd > 0) {
                        SystemData.saveCurrentMana(sPlayer, Math.min(maxCap, currentMana + toAdd));
                    }
                    sPlayer.getPersistentData().putDouble("manhwamod.mana_regen_buffer", newBuffer);
                } else if (currentMana > maxCap) {
                    SystemData.saveCurrentMana(sPlayer, maxCap);
                }
            }

            // 2. Daily Quest Movement Tracking
            double currentDist = player.walkDist;
            double lastDist = player.getPersistentData().getDouble("manhwamod.last_dist");
            if (currentDist > lastDist) {
                DailyQuestData.addDistance(player, currentDist - lastDist);
                player.getPersistentData().putDouble("manhwamod.last_dist", currentDist);
            }

            // 3. Daily Reset Check
            DailyQuestData.checkAndReset(player);
        }
    }

    public static void executeDash(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer sPlayer)) return;

        int speedStat = SystemData.getSpeed(player);
        int currentMana = SystemData.getCurrentMana(player);
        double distanceGoal = Math.min(15.0, 5.0 + (speedStat * 0.334));
        int dashCost = (int) (distanceGoal * 2);

        if (speedStat >= 5 && currentMana >= dashCost) {
            SystemData.saveCurrentMana(player, currentMana - dashCost);
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * (distanceGoal * 0.16), 0.05, look.z * (distanceGoal * 0.16));
            player.hurtMarked = true;
            SystemData.sync(sPlayer);
        }
    }

    private static void updatePlayerRank(ServerPlayer player) {
        int level = player.getPersistentData().getInt("manhwamod.level");
        String currentRank = player.getPersistentData().getString("manhwamod.rank");
        String newRank = "E";

        if (level >= 900) newRank = "SSS";
        else if (level >= 750) newRank = "SS";
        else if (level >= 600) newRank = "S";
        else if (level >= 450) newRank = "A";
        else if (level >= 300) newRank = "B";
        else if (level >= 150) newRank = "C";
        else if (level >= 50) newRank = "D";

        if (!newRank.equals(currentRank)) {
            player.getPersistentData().putString("manhwamod.rank", newRank);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[SYSTEM] §fRank Up: §e§l" + newRank), false);
            SystemData.sync(player);
        }
    }
}