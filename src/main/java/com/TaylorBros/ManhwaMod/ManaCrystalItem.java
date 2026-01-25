package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ManaCrystalItem extends Item {
    public ManaCrystalItem(Properties properties) {
        super(properties);
    }

    // --- MERGED MODE 1: OPEN PERSONAL MENU ---
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            if (SystemData.isAwakened(player)) {
                boolean isSystemPlayer = player.getPersistentData().getBoolean("manhwamod.is_system_player");

                // Business Logic: Route to the correct UI based on User Tier
            } else {
                player.sendSystemMessage(Component.literal("§c[!] The crystal remains dim. You are not awakened."));
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    // --- MODE 2: EVALUATE OTHERS ---
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            // Business Logic: Evaluation is gated by the player's level
            int userLevel = player.getPersistentData().getInt("manhwamod.level");
            int targetLevel = target.getPersistentData().getInt("manhwamod.level");

            if (targetLevel > userLevel + 10) { // Allowed some leeway for better UX
                player.sendSystemMessage(Component.literal("§c[!] Evaluation Failed: Target rank is too high."));
                return InteractionResult.SUCCESS;
            }

            Minecraft.getInstance().setScreen(new EvaluationScreen(target));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Makes the crystal glow like a magical artifact
    }
}