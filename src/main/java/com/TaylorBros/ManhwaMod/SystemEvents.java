package com.TaylorBros.ManhwaMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ManhwaMod.MODID)
public class SystemEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // INITIALIZE NEW PLAYERS
            if (!player.getPersistentData().contains("manhwamod.points")) {
                player.getPersistentData().putInt("manhwamod.points", 5);
                player.getPersistentData().putInt("manhwamod.strength", 10);
                player.getPersistentData().putInt("manhwamod.health_stat", 10);

                // CRITICAL FIX: Start with 100 Mana Stat AND 100 Current Mana
                // This ensures "100/100" on first login.
                player.getPersistentData().putInt(SystemData.MANA, 100);
                player.getPersistentData().putInt(SystemData.CURRENT_MANA, 100);

                player.getPersistentData().putInt("manhwamod.speed", 10);
                player.getPersistentData().putInt("manhwamod.defense", 10);
                player.getPersistentData().putBoolean("manhwamod.awakened", false);
                player.getPersistentData().putBoolean("manhwamod.is_system_player", false);
            }
            SystemData.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            ServerPlayer player = (ServerPlayer) event.player;

            // BUSINESS LOGIC: The Stat (e.g. 200) IS the Limit.
            int maxMana = SystemData.getMana(player);
            int current = SystemData.getCurrentMana(player);

            if (current > maxMana) {
                // SNAP-BACK: If you are at 201/200, snap back to 200.
                SystemData.saveCurrentMana(player, maxMana);
            } else if (current < maxMana) {
                // Regen 1 per tick until you hit the limit
                SystemData.saveCurrentMana(player, current + 1);
            }
        }
    }

    public static void executeDash(Player player) {
        // BUSINESS LOGIC: Only run on the server to prevent desync
        if (player.level().isClientSide) return;

        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (!player.onGround() || !player.getPersistentData().getBoolean("manhwamod.awakened")) return;

        int speedStat = SystemData.getSpeed(player);
        int currentMana = player.getPersistentData().getInt("manhwamod.current_mana");

        // Simple scaling: 5 base + speed influence
        double distanceGoal = Math.min(15.0, 5.0 + (speedStat * 0.334));
        int dashCost = (int) (distanceGoal * 2);

        if (speedStat >= 5 && currentMana >= dashCost) {
            player.getPersistentData().putInt("manhwamod.current_mana", currentMana - dashCost);
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * (distanceGoal * 0.16), 0.05, look.z * (distanceGoal * 0.16));
            player.hurtMarked = true;
            SystemData.sync(player);
        }
    }
}