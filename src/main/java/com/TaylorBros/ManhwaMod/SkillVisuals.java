package com.TaylorBros.ManhwaMod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// LODESTONE IMPORTS
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

import java.awt.*;

public class SkillVisuals {

    public static void play(Player player, String skillName, String fullRecipeData) {
        Level level = player.level();

        // 1. DATA PARSING
        String[] parts = fullRecipeData.split(":");
        String shapeName = parts.length > 0 ? parts[0].trim().toUpperCase() : "SINGLE";
        String elName = parts.length > 1 ? parts[1].split("\\|")[0].trim().toUpperCase() : "FORCE";
        SkillRanker.Rank rank = SkillRanker.getRank(fullRecipeData);
        float scale = 1.0f + (rank.ordinal() * 0.5f);

        // 2. MATH
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

        // 3. COLORS & VANILLA PARTICLES
        Color c1 = getElementColor(elName);
        Color c2 = getFadeColor(elName);
        SimpleParticleType vanillaParticle = getVanillaParticle(elName);

        // 4. SOUND EFFECTS
        playRankSound(level, player, rank, shapeName);

        if (level instanceof ServerLevel) {
            // Screen Shake for high ranks
            if (rank.ordinal() >= SkillRanker.Rank.A.ordinal()) {
                float shake = rank.ordinal() >= SkillRanker.Rank.S.ordinal() ? 1.5f : 0.5f;
                for (Player p : level.players()) {
                    if (p.distanceToSqr(player) < 900) Messages.sendToPlayer(new PacketScreenShake(shake, 20), (ServerPlayer) p);
                }
            }
        }

        // ==================================================
        //            MASTER VISUAL LOGIC
        // ==================================================

        // --- 1. SLASHES (Crescent Shape) ---
        if (shapeName.contains("SLASH")) {
            boolean vert = shapeName.contains("VERT");
            boolean horiz = shapeName.contains("HORIZ");

            double speed = 2.0;
            int lifetime = 25;

            // Spawn slightly forward so it doesn't clip inside head
            Vec3 spawnPos = eyePos.add(look.scale(1.0));

            // ROTATION FIX: 0.0f makes the Apex (Curve) face forward.
            float rotationCorrection = 0.0f;

            float yawRad = (float) Math.toRadians(-player.getYRot());
            float baseAngle = vert ? 0.0f : (horiz ? 1.57f : 0.78f);
            float finalAngle = yawRad + baseAngle + rotationCorrection;

            // Smaller, sharper size
            float width = 1.5f * scale;
            float length = 3.0f * scale;

            // Lodestone Slash
            WorldParticleBuilder.create(ModParticles.CRESCENT_SLASH.get())
                    .setTransparencyData(GenericParticleData.create(1.0f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(width, length).build())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setSpinData(SpinParticleData.create(0, 0).setSpinOffset(finalAngle).build())
                    .setMotion(look.x * speed, look.y * speed, look.z * speed)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setLifetime(lifetime)
                    .spawn(level, spawnPos.x, spawnPos.y, spawnPos.z);

            // Vanilla Trail
            for (int i = 0; i < 5; i++) {
                level.addParticle(vanillaParticle, spawnPos.x, spawnPos.y, spawnPos.z, look.x * speed * 0.5, look.y * speed * 0.5, look.z * speed * 0.5);
            }
        }

        // --- 2. LIGHTNING BOLT (Vertical Strike) ---
        else if (shapeName.contains("BOLT")) {
            Vec3 strikePos = eyePos.add(look.scale(4.0)); // 4 blocks away

            // Core Bolt
            WorldParticleBuilder.create(ModParticles.LIGHTNING_BOLT.get())
                    .setTransparencyData(GenericParticleData.create(1.0f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(1.0f * scale, 8.0f * scale).build()) // Tall
                    .setColorData(ColorParticleData.create(c1, Color.WHITE).build())
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setLifetime(8)
                    .spawn(level, strikePos.x, strikePos.y + 1, strikePos.z);

            // Vanilla Sparks at impact point
            for(int i=0; i<10; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, strikePos.x, strikePos.y, strikePos.z, (Math.random()-0.5), (Math.random()-0.5), (Math.random()-0.5));
            }
        }

        // --- 3. MAGIC CIRCLE (Floor Rune) ---
        else if (shapeName.contains("CIRCLE") || shapeName.contains("AOE")) {
            Vec3 floorPos = player.position().add(0, 0.1, 0); // At feet

            // The Rune Ring
            WorldParticleBuilder.create(ModParticles.MAGIC_CIRCLE.get())
                    .setTransparencyData(GenericParticleData.create(0.8f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(4.0f * scale, 4.0f * scale).build())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setSpinData(SpinParticleData.create(0.05f, 0.1f).build())
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    // IMPORTANT: To make it flat, we rotate the emitter, but Lodestone particles are billboards.
                    // For a flat circle, you usually need a specialized render type or model.
                    // However, we can fake it by spawning a "Cloud" of particles in a ring shape instead if the PNG doesn't lay flat.
                    // For this specific texture, assuming it's billboarded, we rely on the 2D look facing the player.
                    .setLifetime(40)
                    .spawn(level, floorPos.x, floorPos.y + 1, floorPos.z);

            // Upward Energy Column
            for(int i=0; i<8; i++) {
                WorldParticleBuilder.create(ModParticles.SOFT_GLOW.get())
                        .setScaleData(GenericParticleData.create(1.0f, 0).build())
                        .setColorData(ColorParticleData.create(c1, new Color(0,0,0,0)).build())
                        .setMotion(0, 0.3, 0)
                        .setLifetime(25)
                        .spawn(level, floorPos.x + (Math.random()-0.5)*3, floorPos.y, floorPos.z + (Math.random()-0.5)*3);
            }
        }

        // --- 4. SMOKE / WALL (Thick Cloud) ---
        else if (shapeName.contains("SMOKE") || shapeName.contains("WALL")) {
            Vec3 center = eyePos.add(look.scale(3.0));
            for(int i=-2; i<=2; i++) {
                Vec3 p = center.add(right.scale(i * 1.5));
                WorldParticleBuilder.create(ModParticles.SMOKE_CLOUD.get())
                        .setScaleData(GenericParticleData.create(4.0f * scale, 6.0f * scale).build())
                        .setColorData(ColorParticleData.create(c1, Color.BLACK).build())
                        .setTransparencyData(GenericParticleData.create(0.8f, 0.0f).build())
                        .setSpinData(SpinParticleData.create(0.05f, 0.1f).build())
                        .setLifetime(60)
                        .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                        .spawn(level, p.x, p.y, p.z);
            }
        }

        // --- 5. SPARK / PUNCH (Burst) ---
        else if (shapeName.contains("SPARK") || shapeName.contains("PUNCH") || shapeName.contains("STAR")) {
            Vec3 pos = eyePos.add(look.scale(1.5));

            // Star Flare Core
            WorldParticleBuilder.create(ModParticles.STAR_FLARE.get())
                    .setColorData(ColorParticleData.create(c1, Color.WHITE).build())
                    .setScaleData(GenericParticleData.create(2.0f * scale, 0).build())
                    .setLifetime(10)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, pos.x, pos.y, pos.z);

            // Explosive Sparks
            for(int i=0; i<15; i++) {
                double mx = (Math.random()-0.5); double my = (Math.random()-0.5); double mz = (Math.random()-0.5);
                WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                        .setColorData(ColorParticleData.create(c1, c2).build())
                        .setScaleData(GenericParticleData.create(0.5f * scale, 0).build())
                        .setMotion(mx, my, mz)
                        .setLifetime(10)
                        .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                        .spawn(level, pos.x, pos.y, pos.z);
            }
        }

        // --- 6. BEAM (Ray) ---
        else if (shapeName.contains("BEAM") || shapeName.contains("RAY")) {
            for(int i=0; i<25; i++) {
                Vec3 p = eyePos.add(look.scale(i * 1.2));
                WorldParticleBuilder.create(ModParticles.SOFT_GLOW.get())
                        .setScaleData(GenericParticleData.create(0.8f * scale, 0).build())
                        .setColorData(ColorParticleData.create(c1, c2).build())
                        .setLifetime(10)
                        .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                        .spawn(level, p.x, p.y, p.z);
            }
            // Add helix swirl
            spawnHelix(level, eyePos, look, c2, scale);
        }

        // --- 7. BALL / FLARE (Projectile) ---
        else if (shapeName.contains("BALL") || shapeName.contains("FLARE") || shapeName.contains("SINGLE")) {
            Vec3 pos = eyePos.add(look.scale(2.0));
            double speed = 1.0;

            WorldParticleBuilder.create(ModParticles.SHOCKWAVE_RING.get())
                    .setScaleData(GenericParticleData.create(3.0f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setMotion(look.x * speed, look.y * speed, look.z * speed)
                    .setLifetime(20)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, pos.x, pos.y, pos.z);

            WorldParticleBuilder.create(ModParticles.SOFT_GLOW.get())
                    .setScaleData(GenericParticleData.create(1.0f * scale, 2.0f * scale).build())
                    .setColorData(ColorParticleData.create(Color.WHITE, c1).build())
                    .setMotion(look.x * speed, look.y * speed, look.z * speed)
                    .setLifetime(20)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, pos.x, pos.y, pos.z);

            level.addParticle(vanillaParticle, pos.x, pos.y, pos.z, look.x * 0.5, look.y * 0.5, look.z * 0.5);
        }

        // --- DEFAULT FALLBACK ---
        else {
            spawnHelix(level, eyePos, look, c1, scale);
        }
    }

    // --- HELPER METHODS ---

    private static void spawnHelix(Level level, Vec3 start, Vec3 dir, Color color, float scale) {
        Vec3 right = dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(dir).normalize();
        for (int i = 0; i < 40; i++) {
            double angle = i * 0.5;
            double r = 0.5 * scale;
            Vec3 off = right.scale(Math.cos(angle)*r).add(up.scale(Math.sin(angle)*r));
            Vec3 p = start.add(dir.scale(i * 0.5)).add(off);
            WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                    .setColorData(ColorParticleData.create(color, Color.BLACK).build())
                    .setScaleData(GenericParticleData.create(0.2f, 0).build())
                    .setLifetime(15)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .spawn(level, p.x, p.y, p.z);
        }
    }

    private static void playRankSound(Level level, Player player, SkillRanker.Rank rank, String shapeName) {
        // 1. Determine Base Sound by Shape
        if (shapeName.contains("SLASH")) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        } else if (shapeName.contains("BOLT")) {
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.0f);
        } else if (shapeName.contains("EXPLODE") || shapeName.contains("IMPACT")) {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.0f);
        }

        // 2. Rank Bonus Sound
        if(rank.ordinal() >= SkillRanker.Rank.S.ordinal()) {
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.2f, 1.0f);
        } else if (rank.ordinal() >= SkillRanker.Rank.B.ordinal()) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
        }
    }

    private static Color getElementColor(String elName) {
        return switch (elName) {
            case "FIRE", "LAVA" -> new Color(255, 100, 0);
            case "ICE", "WATER" -> new Color(100, 200, 255);
            case "LIGHTNING" -> new Color(200, 255, 50);
            case "VOID", "SHADOW" -> new Color(20, 0, 40);
            case "FORCE", "LIGHT" -> Color.WHITE;
            case "POISON", "ACID", "WIND" -> new Color(100, 255, 50);
            case "EARTH" -> new Color(139, 69, 19);
            default -> Color.WHITE;
        };
    }

    private static Color getFadeColor(String elName) {
        return switch (elName) {
            case "FIRE" -> new Color(255, 200, 0);
            case "VOID" -> new Color(100, 0, 200);
            case "SHADOW" -> new Color(50, 0, 0);
            case "ICE" -> new Color(200, 255, 255);
            default -> Color.WHITE;
        };
    }

    private static SimpleParticleType getVanillaParticle(String elName) {
        return switch (elName) {
            case "FIRE", "LAVA" -> ParticleTypes.FLAME;
            case "ICE", "WATER" -> ParticleTypes.SNOWFLAKE;
            case "LIGHTNING" -> ParticleTypes.ELECTRIC_SPARK;
            case "VOID", "SHADOW" -> ParticleTypes.SQUID_INK;
            case "POISON", "ACID" -> ParticleTypes.ENTITY_EFFECT;
            case "EARTH" -> ParticleTypes.CRIT;
            case "WIND" -> ParticleTypes.CLOUD;
            case "FORCE" -> ParticleTypes.ENCHANTED_HIT;
            default -> ParticleTypes.CRIT;
        };
    }
}