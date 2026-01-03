package com.TaylorBros.ManhwaMod;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SystemKeyItem extends Item {
    public SystemKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 1. Business Logic: Check if already awakened
            if (SystemData.isAwakened(player)) {
                player.sendSystemMessage(Component.literal("§c[!] You have already been authenticated by the System."));
                return InteractionResultHolder.fail(stack);
            }

            // 2. Core Awakening & Roll
            SystemData.saveAwakening(player, true);
            boolean isProtagonist = level.random.nextFloat() < 0.10f; // 10% chance
            player.getPersistentData().putBoolean("manhwamod.is_system_player", isProtagonist);

            // 3. Initialize Stats
            player.getPersistentData().putInt("manhwamod.level", 1);
            player.getPersistentData().putInt("manhwamod.current_mana", 20);

            // CRITICAL: Sync data so the G-key works immediately
            SystemData.sync(player);

            // 4. Feedback Logic
            if (isProtagonist) {
                // Trigger the Cinematic Animation via a Packet
                Messages.sendToPlayer(new PacketOpenBootAnimation(), (ServerPlayer) player);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

                player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fAuthentication Complete."));
                player.sendSystemMessage(Component.literal("§e------------------------------------------"));
                player.sendSystemMessage(Component.literal("§fWelcome, §bPlayer§f. System interface initialized."));
                player.sendSystemMessage(Component.literal("§7(Press §b'G' §7to view your Status)"));
                player.sendSystemMessage(Component.literal("§e------------------------------------------"));
            } else {
                player.sendSystemMessage(Component.literal("§6§l[AWAKENING] §fSuccess. Rank: §eE-Rank§f."));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}