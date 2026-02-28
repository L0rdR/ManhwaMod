package com.TaylorBros.ManhwaMod;

import com.TaylorBros.ManhwaMod.SkillEngine;
import com.TaylorBros.ManhwaMod.SkillTags;
import com.TaylorBros.ManhwaMod.SystemData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

import java.lang.reflect.Field;
import java.util.List;

public class ManhwaSkill extends Skill {
    private final SkillTags.Shape shape;
    private final float triggerPct;

    // Reflection Setup
    private static Field durationField;
    private static Field maxDurationField;

    static {
        try {
            durationField = SkillContainer.class.getDeclaredField("duration");
            durationField.setAccessible(true);
            maxDurationField = SkillContainer.class.getDeclaredField("maxDuration");
            maxDurationField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access Epic Fight SkillContainer fields", e);
        }
    }

    public ManhwaSkill(Builder<? extends Skill> builder, SkillTags.Shape shape, float triggerPct) {
        super(builder);
        this.shape = shape;
        this.triggerPct = triggerPct;
    }

    @Override
    public boolean checkExecuteCondition(PlayerPatch<?> executer) {
        if (!super.checkExecuteCondition(executer)) return false;

        if (!executer.isLogicalClient()) {
            ServerPlayer player = (ServerPlayer) executer.getOriginal();

            // 1. Check if we have a recipe for this shape in our slots
            String foundRecipe = findRecipeForShape(player);
            if (foundRecipe == null) {
                player.displayClientMessage(Component.literal("§cYou haven't equipped a " + shape.name() + " art in your Manhwa slots!"), true);
                return false;
            }

            // 2. Check Mana (Estimation, real check happens in execute)
            if (SystemData.getCurrentMana(player) < 20) {
                player.displayClientMessage(Component.literal("§cNot enough Mana!"), true);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);

        PlayerPatch<?> executer = container.getExecuter();
        StaticAnimation animToPlay = findAnimationForShape(executer, this.shape);

        if (animToPlay != null) {
            executer.playAnimationSynchronized(animToPlay, 0);
            int animTicks = (int) (animToPlay.getTotalTime() * 20);
            container.setMaxDuration(animTicks);
            container.setDuration(animTicks);
        } else {
            container.setMaxDuration(20);
            container.setDuration(20);
        }
    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        if (container.getExecuter().isLogicalClient()) return;

        try {
            int currentDuration = durationField.getInt(container);
            int maxDuration = maxDurationField.getInt(container);
            int fireTick = (int) (maxDuration * (1.0f - this.triggerPct));

            if (currentDuration == fireTick) {
                ServerPlayer player = (ServerPlayer) container.getExecuter().getOriginal();
                executeManhwaPayload(player);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void executeManhwaPayload(ServerPlayer player) {
        // DYNAMIC LOOKUP: Find the recipe the player actually has equipped
        String recipe = findRecipeForShape(player);

        if (recipe != null) {
            SkillEngine.castImmediate(player, recipe, this.shape);
        }
    }

    // Scans SystemData slots (0-4) to find a recipe matching this skill's SHAPE
    private String findRecipeForShape(ServerPlayer player) {
        for (int i = 0; i < 5; i++) {
            String recipe = SystemData.getSkillRecipe(player, i);
            if (recipe != null && !recipe.isEmpty()) {
                // Recipe format: "SHAPE:ELEMENT:MODIFIER"
                String cleanRecipe = recipe.contains("|") ? recipe.split("\\|")[0] : recipe;
                String[] parts = cleanRecipe.split(":");
                if (parts.length > 0) {
                    // Check if "BLINK_STRIKE" matches this.shape.name()
                    if (parts[0].equalsIgnoreCase(this.shape.name())) {
                        return recipe;
                    }
                }
            }
        }
        return null;
    }

    private StaticAnimation findAnimationForShape(PlayerPatch<?> patch, SkillTags.Shape shape) {
        ItemStack held = patch.getOriginal().getMainHandItem();
        CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(held);

        if (itemCap instanceof WeaponCapability weaponCap) {
            List<StaticAnimation> autos = weaponCap.getAutoAttckMotion(patch);
            if (autos.isEmpty()) return null;

            if (shape == SkillTags.Shape.HORIZ_SLASH) return autos.get(0);
            if (shape == SkillTags.Shape.SLASH) return autos.size() > 1 ? autos.get(1) : autos.get(0);
            if (shape == SkillTags.Shape.VERT_SLASH) return autos.size() > 2 ? autos.get(2) : autos.get(autos.size() - 1);
            if (shape == SkillTags.Shape.DASH || shape == SkillTags.Shape.BLINK_STRIKE) return autos.get(0);
            if (shape == SkillTags.Shape.SLASH_BARRAGE) return autos.size() > 1 ? autos.get(1) : autos.get(0);

            return autos.get(0);
        }
        return null;
    }
}