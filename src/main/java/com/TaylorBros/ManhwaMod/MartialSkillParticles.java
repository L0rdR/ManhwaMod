package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

import java.awt.Color;

import com.TaylorBros.ManhwaMod.ModParticles;
import com.TaylorBros.ManhwaMod.SkillTags;

public class MartialSkillParticles {

    // ENTRY POINT
    public static void spawnSlash(Player player, SkillTags.Shape shape, SkillTags.Element element) {
        if (player == null) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // Always in front of player
        Vec3 pos = eye.add(look.scale(2.0));

        // Orientation
        float yaw = (float) Math.toRadians(-player.getYRot());

        float baseAngle =
                shape == SkillTags.Shape.VERT_SLASH ? 0f :
                        shape == SkillTags.Shape.HORIZ_SLASH ? (float) Math.PI / 2f :
                                (float) Math.PI / 4f;

        float angle = yaw + baseAngle;

        Color c1 = getPrimary(element);
        Color c2 = getFade(element);

        WorldParticleBuilder.create(ModParticles.CRESCENT_SLASH.get())
                .setTransparencyData(GenericParticleData.create(0.9f, 0.0f).build())
                .setScaleData(GenericParticleData.create(3.0f, 6.0f).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setSpinData(SpinParticleData.create(0, 0).setSpinOffset(angle).build())
                .setMotion(look.x * 0.15, look.y * 0.15, look.z * 0.15)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setLifetime(10)
                .spawn(player.level(), pos.x, pos.y, pos.z);
    }

    // ---------------- COLORS ----------------

    private static Color getPrimary(SkillTags.Element e) {
        return switch (e) {
            case FIRE -> new Color(255, 120, 20);
            case ICE -> new Color(150, 220, 255);
            case LIGHTNING -> new Color(220, 120, 255);
            case WIND -> new Color(200, 255, 255);
            case SHADOW -> new Color(30, 30, 30);
            default -> Color.WHITE;
        };
    }

    private static Color getFade(SkillTags.Element e) {
        return switch (e) {
            case FIRE -> new Color(255, 200, 80);
            case SHADOW -> new Color(80, 0, 0);
            default -> Color.WHITE;
        };
    }
}
