package com.TaylorBros.ManhwaMod;

import net.minecraft.ChatFormatting;
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

public class SkillOrbItem extends Item {
    public SkillOrbItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 1. Check if Orb has data
            if (!stack.hasTag() || !stack.getTag().contains("StoredSkillRecipe")) {
                player.displayClientMessage(Component.literal("§7[This Orb is empty. Use the Hunter Phone to extract a skill.]"), true);
                return InteractionResultHolder.fail(stack);
            }

            // 2. Read Data
            String recipe = stack.getTag().getString("StoredSkillRecipe");
            int cost = stack.getTag().getInt("StoredSkillCost");
            int newId = 50000 + level.random.nextInt(50000);

            // 3. Learn Logic
            List<Integer> currentSkills = SystemData.getUnlockedSkills(player);
            boolean alreadyHas = false;
            for(int id : currentSkills) {
                String r = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + id);
                if (r.equals(recipe)) alreadyHas = true;
            }

            if (alreadyHas) {
                player.sendSystemMessage(Component.literal("§c[!] You already know this Technique."));
                return InteractionResultHolder.fail(stack);
            }

            // 4. Success
            SystemData.unlockSkill(player, newId, recipe, cost);
            player.sendSystemMessage(Component.literal("§a[SYSTEM] §fTechnique Absorbed Successfully."));

            // --- THE FIX: CONSUME THE ITEM ---
            stack.shrink(1); // Reduces count by 1 (Destroys it)
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (stack.hasTag() && stack.getTag().contains("StoredSkillRecipe")) {
            String fullData = stack.getTag().getString("StoredSkillRecipe");
            String displayName = fullData.contains("|") ? fullData.split("\\|")[1] : fullData;
            SkillRanker.Rank rank = SkillRanker.getRank(fullData);

            tooltipComponents.add(Component.literal("§7Contains Technique:"));
            tooltipComponents.add(Component.literal(" [" + rank.label + "] " + displayName).withStyle(style -> style.withColor(rank.color)));
            tooltipComponents.add(Component.literal("§8(Right-Click to Learn)"));
        } else {
            tooltipComponents.add(Component.literal("§7Empty. Ready for extraction."));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("StoredSkillRecipe");
    }
}