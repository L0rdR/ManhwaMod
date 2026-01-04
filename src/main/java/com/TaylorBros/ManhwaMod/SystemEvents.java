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

                // Start with 100 Mana (10 points invested initially)
                player.getPersistentData().putInt(SystemData.MANA, 100);
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

            // BUSINESS LOGIC: 1 Stat Point = 10 Max Mana
            int manaStat = SystemData.getMana(player); // This is 20
            int maxCap = manaStat * 10;               // This makes the capacity 200

            int currentMana = SystemData.getCurrentMana(player);

            if (currentMana < maxCap) {
                SystemData.saveCurrentMana(player, currentMana + 1);
            } else if (currentMana > maxCap) {
                SystemData.saveCurrentMana(player, maxCap); // Force back to cap
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
}