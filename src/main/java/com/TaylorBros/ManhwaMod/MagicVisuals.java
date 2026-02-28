package com.TaylorBros.ManhwaMod;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

/**
 * Non-martial / mana skill visuals.
 * This is intentionally conservative: it won't touch your SkillEngine, damage, cooldowns, etc.
 * It only draws particles on the client when the cast packet arrives.
 */
public class MagicVisuals {

    public static void play(Player player, String shapeName, String elName, Color c1, Color c2, float scale, SkillRanker.Rank rank) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 forward = player.getLookAngle().normalize();

        // Simple, readable defaults
        if (shapeName.contains("BEAM") || shapeName.contains("RAY")) {
            beam(level, eye, forward, c1, c2, scale);
            return;
        }

        if (shapeName.contains("BALL") || shapeName.contains("FLARE") || shapeName.contains("SINGLE")) {
            projectileBurst(level, eye, forward, c1, c2, scale);
            return;
        }

        if (shapeName.contains("CIRCLE") || shapeName.contains("AOE")) {
            circle(level, player.position(), c1, c2, scale);
            return;
        }

        if (shapeName.contains("SMOKE") || shapeName.contains("WALL")) {
            smokeWall(level, eye, forward, c1, c2, scale);
            return;
        }

        if (shapeName.contains("SPARK") || shapeName.contains("STAR") || shapeName.contains("PUNCH")) {
            impact(level, eye.add(forward.scale(1.6)), c1, c2, scale);
            return;
        }

        // Fallback: one small sparkle so we always see *something*
        ParticleOptions fallback = SkillVisuals.getVanillaParticleFallback(elName);
        level.addParticle(fallback, player.getX(), player.getEyeY(), player.getZ(), 0, 0, 0);
    }

    private static void beam(Level level, Vec3 eye, Vec3 forward, Color c1, Color c2, float scale) {
        for (int i = 0; i < 22; i++) {
            Vec3 p = eye.add(forward.scale(i * 1.0));
            WorldParticleBuilder.create(ModParticles.SOFT_GLOW.get())
                    .setScaleData(GenericParticleData.create(0.55f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setTransparencyData(GenericParticleData.create(0.7f, 0.0f).build())
                    .setLifetime(10)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, p.x, p.y, p.z);
        }
        helix(level, eye, forward, c2, scale);
    }

    private static void projectileBurst(Level level, Vec3 eye, Vec3 forward, Color c1, Color c2, float scale) {
        Vec3 pos = eye.add(forward.scale(2.0));
        WorldParticleBuilder.create(ModParticles.SHOCKWAVE_RING.get())
                .setScaleData(GenericParticleData.create(2.8f * scale, 0.0f).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setTransparencyData(GenericParticleData.create(0.8f, 0.0f).build())
                .setLifetime(18)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .spawn(level, pos.x, pos.y, pos.z);

        for (int i = 0; i < 12; i++) {
            double mx = (Math.random() - 0.5) * 0.7;
            double my = (Math.random() - 0.5) * 0.5;
            double mz = (Math.random() - 0.5) * 0.7;
            WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                    .setScaleData(GenericParticleData.create(0.28f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setTransparencyData(GenericParticleData.create(0.9f, 0.0f).build())
                    .setMotion(mx + forward.x * 0.05, my + forward.y * 0.05, mz + forward.z * 0.05)
                    .setLifetime(12)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, pos.x, pos.y, pos.z);
        }
    }

    private static void circle(Level level, Vec3 center, Color c1, Color c2, float scale) {
        Vec3 floor = center.add(0, 0.1, 0);

        WorldParticleBuilder.create(ModParticles.SHOCKWAVE_RING.get())
                .setScaleData(GenericParticleData.create(5.0f * scale, 0.0f).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setTransparencyData(GenericParticleData.create(0.55f, 0.0f).build())
                .setSpinData(SpinParticleData.create(0.02f, 0.05f).build())
                .setLifetime(35)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .spawn(level, floor.x, floor.y + 0.6, floor.z);

        for (int i = 0; i < 10; i++) {
            WorldParticleBuilder.create(ModParticles.SOFT_GLOW.get())
                    .setScaleData(GenericParticleData.create(0.9f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(c1, new Color(0, 0, 0, 0)).build())
                    .setTransparencyData(GenericParticleData.create(0.6f, 0.0f).build())
                    .setMotion(0, 0.20, 0)
                    .setLifetime(22)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, floor.x + (Math.random() - 0.5) * 3.0, floor.y, floor.z + (Math.random() - 0.5) * 3.0);
        }
    }

    private static void smokeWall(Level level, Vec3 eye, Vec3 forward, Color c1, Color c2, float scale) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 center = eye.add(forward.scale(3.0));

        for (int i = -2; i <= 2; i++) {
            Vec3 p = center.add(right.scale(i * 1.4));
            WorldParticleBuilder.create(ModParticles.SMOKE_CLOUD.get())
                    .setScaleData(GenericParticleData.create(3.8f * scale, 6.0f * scale).build())
                    .setColorData(ColorParticleData.create(c1, Color.BLACK).build())
                    .setTransparencyData(GenericParticleData.create(0.75f, 0.0f).build())
                    .setSpinData(SpinParticleData.create(0.03f, 0.08f).build())
                    .setLifetime(55)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, p.x, p.y, p.z);
        }
    }

    private static void impact(Level level, Vec3 pos, Color c1, Color c2, float scale) {
        WorldParticleBuilder.create(ModParticles.SOFT_GLOW.get())
                .setScaleData(GenericParticleData.create(1.3f * scale, 0.0f).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setTransparencyData(GenericParticleData.create(0.85f, 0.0f).build())
                .setLifetime(10)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .spawn(level, pos.x, pos.y, pos.z);

        for (int i = 0; i < 14; i++) {
            double mx = (Math.random() - 0.5) * 0.9;
            double my = (Math.random() - 0.5) * 0.7;
            double mz = (Math.random() - 0.5) * 0.9;
            WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                    .setScaleData(GenericParticleData.create(0.30f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setTransparencyData(GenericParticleData.create(0.9f, 0.0f).build())
                    .setMotion(mx, my, mz)
                    .setLifetime(10)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, pos.x, pos.y, pos.z);
        }
    }

    private static void helix(Level level, Vec3 start, Vec3 dir, Color color, float scale) {
        Vec3 right = dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(dir).normalize();

        for (int i = 0; i < 30; i++) {
            double angle = i * 0.55;
            double r = 0.45 * scale;

            Vec3 off = right.scale(Math.cos(angle) * r).add(up.scale(Math.sin(angle) * r));
            Vec3 p = start.add(dir.scale(i * 0.55)).add(off);

            WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                    .setScaleData(GenericParticleData.create(0.22f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(color, Color.BLACK).build())
                    .setTransparencyData(GenericParticleData.create(0.85f, 0.0f).build())
                    .setLifetime(14)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, p.x, p.y, p.z);
        }
    }
}
