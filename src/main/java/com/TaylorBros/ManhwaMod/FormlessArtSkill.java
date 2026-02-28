package com.TaylorBros.ManhwaMod.skills;

import com.TaylorBros.ManhwaMod.ManhwaMod; // Needed for MODID
import com.TaylorBros.ManhwaMod.SkillEngine;
import com.TaylorBros.ManhwaMod.SkillTags;
import com.TaylorBros.ManhwaMod.SystemData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.api.animation.types.StaticAnimation;

import java.util.List;

public class FormlessArtSkill extends Skill {

    public FormlessArtSkill(Builder<? extends Skill> builder) {
        super(builder);
    }

    @Override
    public boolean checkExecuteCondition(PlayerPatch<?> executer) {
        return true;
    }

    @Override
    public void onInitiate(SkillContainer container) {
        PlayerPatch<?> playerPatch = container.getExecuter();
        Player player = playerPatch.getOriginal();
        ItemStack heldItem = player.getMainHandItem();

        String technique = SystemData.getMemorizedTechnique(player);
        if (technique.isEmpty()) {
            player.displayClientMessage(Component.literal("§c[System] No technique memorized."), true);
            return;
        }

        String shapeName = technique.split(":")[0];
        SkillTags.Shape shape;
        try {
            shape = SkillTags.Shape.valueOf(shapeName.toUpperCase());
        } catch (Exception e) {
            shape = SkillTags.Shape.SLASH;
        }

        StaticAnimation animToPlay = getAnimationForShape(playerPatch, heldItem, shape);

        if (animToPlay != null) {
            playerPatch.playAnimationSynchronized(animToPlay, 0);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SkillEngine.castImmediate(serverPlayer, technique, shape);
        }
    }

    private StaticAnimation getAnimationForShape(PlayerPatch<?> patch, ItemStack stack, SkillTags.Shape shape) {
        CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(stack);
        List<StaticAnimation> autoAttacks = null;

        if (itemCap instanceof WeaponCapability weaponCap) {
            autoAttacks = weaponCap.getAutoAttckMotion(patch);
        }

        if (autoAttacks != null && !autoAttacks.isEmpty()) {
            switch (shape) {
                case SLASH: return autoAttacks.get(0);
                case HORIZ_SLASH: return (autoAttacks.size() > 1) ? autoAttacks.get(1) : autoAttacks.get(0);
                case VERT_SLASH: return autoAttacks.get(autoAttacks.size() - 1);
            }
        }

        return switch (shape) {
            case BLINK_STRIKE -> Animations.BATTOJUTSU_DASH;
            case DASH -> Animations.BIPED_ROLL_FORWARD;
            case IMPACT_BURST -> Animations.GREATSWORD_DASH;
            case SLASH_BARRAGE -> Animations.TACHI_AUTO3;
            case BARRAGE_PUNCH -> Animations.FIST_AUTO3;
            case BARRAGE -> Animations.BIPED_MOB_THROW;
            case PUNCH -> Animations.FIST_AUTO1;
            default -> Animations.SWORD_AUTO1;
        };
    }

    // --- TEXTURE OVERRIDE ---
    @Override
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getSkillTexture() {
        // Points to: assets/manhwamod/textures/item/formless_art.png
        return new ResourceLocation(ManhwaMod.MODID, "textures/item/formless_art.png");
    }
}