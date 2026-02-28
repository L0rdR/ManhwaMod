package com.TaylorBros.ManhwaMod.item;

import com.TaylorBros.ManhwaMod.SystemData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MartialScrollItem extends Item {
    public MartialScrollItem(Properties properties) {
        super(properties);
    }

    // 1. Right-Click to "Memorize" the technique
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Technique")) {
                String technique = tag.getString("Technique");

                // Save to Player Data
                SystemData.setMemorizedTechnique(player, technique);

                player.sendSystemMessage(Component.literal("You have memorized the technique: ")
                        .append(Component.literal(technique).withStyle(ChatFormatting.GOLD)));
            } else {
                player.sendSystemMessage(Component.literal("This scroll is blank.").withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResultHolder.success(stack);
    }

    // 2. Show the technique in the tooltip
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("Technique")) {
            String tech = stack.getTag().getString("Technique");
            tooltip.add(Component.literal("Technique: " + tech).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal("Blank").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}