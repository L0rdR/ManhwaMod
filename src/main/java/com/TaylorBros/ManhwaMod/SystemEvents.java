package com.TaylorBros.ManhwaMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "manhwamod")
public class SystemEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.getPersistentData().contains("manhwamod.points")) {
                player.getPersistentData().putInt("manhwamod.points", 5);
                player.getPersistentData().putInt("manhwamod.strength", 10);
                player.getPersistentData().putInt("manhwamod.health_stat", 10);

                // FIX: Stat is 10, Current Pool is 100
                player.getPersistentData().putInt(SystemData.MANA, 10);
                player.getPersistentData().putInt(SystemData.CURRENT_MANA, 100);

                player.getPersistentData().putInt("manhwamod.speed", 10);
                player.getPersistentData().putInt("manhwamod.defense", 10);
                player.getPersistentData().putBoolean("manhwamod.awakened", false);
            }
            SystemData.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            ServerPlayer player = (ServerPlayer) event.player;

            // 1. Get Core Data
            int manaStat = SystemData.getMana(player);
            int maxCap = manaStat * 10;
            int currentMana = SystemData.getCurrentMana(player);

            // 2. Only regenerate if under the cap
            if (currentMana < maxCap) {
                // SCALING REGEN LOGIC: 0.05 base + 0.005 per stat point
                double regenPerTick = 0.05 + (manaStat / 200.0);

                // Get the partial mana left over from the last tick
                double buffer = player.getPersistentData().getDouble("manhwamod.mana_regen_buffer");
                double totalAddition = regenPerTick + buffer;

                int toAdd = (int) totalAddition; // The whole number to add this tick
                double newBuffer = totalAddition - toAdd; // Save the decimal for the next tick

                if (toAdd > 0) {
                    SystemData.saveCurrentMana(player, Math.min(maxCap, currentMana + toAdd));
                }
                player.getPersistentData().putDouble("manhwamod.mana_regen_buffer", newBuffer);

            } else if (currentMana > maxCap) {
                // HARD CAP SAFETY: Snap back to max if overflow occurs
                SystemData.saveCurrentMana(player, maxCap);
                player.getPersistentData().putDouble("manhwamod.mana_regen_buffer", 0.0);
            }
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

            // 1000 Level Scaling Thresholds
            if (level >= 900) newRank = "SSS";     // Monarch Rank
            else if (level >= 750) newRank = "SS";  // King Rank
            else if (level >= 600) newRank = "S";   // World-Class
            else if (level >= 450) newRank = "A";   // High-Ranker
            else if (level >= 300) newRank = "B";
            else if (level >= 150) newRank = "C";
            else if (level >= 50) newRank = "D";
            else newRank = "E";

            if (!newRank.equals(currentRank)) {
                player.getPersistentData().putString("manhwamod.rank", newRank);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[SYSTEM] §fRank Up: §e§l" + newRank), false);
                SystemData.sync(player);
            }
        }
    }