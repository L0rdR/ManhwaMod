package com.TaylorBros.ManhwaMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

import java.awt.Color;

public class StrikeVisuals {

    // Main entry called by SkillVisuals (client-side)
    public static void play(Player player, String shapeName, Color c1, Color c2, float scale, SkillRanker.Rank rank) {
        if (player == null || player.level() == null) return;

        Level level = player.level();

        Vec3 forward = player.getLookAngle().normalize();
        Vec3 eye = player.getEyePosition();

        // Spawn at what you're aiming at (so it appears "on the target")
        Vec3 center = getHitPos(player, 6.0).add(0, -0.2, 0);

        switch (shapeName) {
            case "SLASH" -> cut(level, center, forward, scale, rank, c1, c2, (float) (Math.PI / 4.0));
            case "VERT_SLASH" -> cut(level, center, forward, scale, rank, c1, c2, 0.0f);
            case "HORIZ_SLASH" -> cut(level, center, forward, scale, rank, c1, c2, (float) (Math.PI / 2.0));

            case "SLASH_BARRAGE" -> barrageStreak(level, eye.add(forward.scale(0.9)), forward, scale, rank, c1, c2);



            case "DASH" -> dashRush(level, eye.add(forward.scale(0.8)), forward, scale, rank, c1, c2);
            case "BLINK_STRIKE" -> {
                // pop at the hit location + a strong cut
                blinkPop(level, center, scale, rank, c1, c2);
                cut(level, center, forward, scale * 1.15f, rank, c1, c2, (float) (Math.PI / 4.0));
            }

            case "PUNCH" -> punch(level, center, forward, scale, rank, c1, c2);
            case "BARRAGE_PUNCH" -> {
                float t = rank.ordinal() / (float) (SkillRanker.Rank.values().length - 1);

                float baseMin = 0.85f - 0.05f * t;
                float baseMax = 0.95f + 0.15f * t;
                float jitterScale = scale * (baseMin + level.random.nextFloat() * (baseMax - baseMin));

                float jitterPos = 0.03f + 0.22f * t;
                Vec3 pos = center.add(
                        (level.random.nextFloat() * 2f - 1f) * jitterPos,
                        (level.random.nextFloat() * 2f - 1f) * (jitterPos * 0.6f),
                        (level.random.nextFloat() * 2f - 1f) * jitterPos
                );

                punch(level, pos, forward, jitterScale, rank, c1, c2);
            }
        }
    }

    /**
     * Used by PacketPlayStrikeImpact: spawns a quick "hit marker" slash over a target entity.
     * This is separate from the player-cast swing visuals.
     */
    public static void playImpact(Level level, int targetEntityId, String fullRecipeData) {
        if (level == null) return;
        var target = level.getEntity(targetEntityId);
        if (target == null) return;

        String clean = (fullRecipeData != null && fullRecipeData.contains("|"))
                ? fullRecipeData.split("\\|")[0]
                : (fullRecipeData == null ? "" : fullRecipeData);
        String[] parts = clean.isEmpty() ? new String[0] : clean.split(":");
        String elName = parts.length > 1 ? parts[1].trim().toUpperCase() : "FORCE";

        SkillRanker.Rank rank = SkillRanker.getRank(fullRecipeData);
        float impactScale = 0.9f + (rank.ordinal() * 0.25f);

        Color c1 = SkillVisuals.getElementColor(elName);
        Color c2 = SkillVisuals.getFadeColor(elName);

        // Slightly above the target's center
        Vec3 pos = target.position().add(0, target.getBbHeight() * 0.7, 0);

        // Random roll so repeated hits look like consecutive cuts
        float roll = level.random.nextFloat() * (float) Math.PI;

        LodestoneWorldParticleType type = pickCut(rank);
        WorldParticleBuilder.create(type)
                .setTransparencyData(GenericParticleData.create(0.95f, 0.0f).build())
                .setScaleData(GenericParticleData.create(1.2f * impactScale, 1.8f * impactScale).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setSpinData(SpinParticleData.create(0.0f, 0.0f).setSpinOffset(roll).build())
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setLifetime(8 + rank.ordinal())
                .spawn(level, pos.x, pos.y, pos.z);
    }
    private static void barrageStreak(Level level, Vec3 start, Vec3 forward, float scale,
                                      SkillRanker.Rank rank, Color c1, Color c2) {

        // Rank -> 0..1
        float t = rank.ordinal() / (float) (SkillRanker.Rank.values().length - 1);

        // Build a little offset plane so streaks don't stack perfectly
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up);
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up2 = right.cross(forward).normalize();

        float side = (level.random.nextFloat() * 2f - 1f) * (0.10f + 0.25f * t);
        float vert = (level.random.nextFloat() * 2f - 1f) * (0.06f + 0.18f * t);

        Vec3 pos = start.add(right.scale(side)).add(up2.scale(vert));

        // BR sprite choice (EpicFight barrage streak style)
        LodestoneWorldParticleType type = pickRush(rank);

        // Random roll so it feels like consecutive swings, not stamped copies
        float roll = (level.random.nextFloat() * 2f - 1f) * (float) Math.PI;

        // Slight forward drift helps sell motion
        double drift = 0.04 + 0.08 * t;

        WorldParticleBuilder.create(type)
                .setTransparencyData(GenericParticleData.create(0.95f, 0.0f).build())
                .setScaleData(GenericParticleData.create((1.0f + 0.6f * t) * scale, (1.0f + 0.6f * t) * scale).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setSpinData(SpinParticleData.create(0.0f, 0.0f).setSpinOffset(roll).build())
                .setMotion(forward.x * drift, forward.y * (drift * 0.35), forward.z * drift)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setLifetime(6 + (int) (6 * t))
                .spawn(level, pos.x, pos.y, pos.z);
    }


    // ---------- Core FX ----------

    private static void cut(Level level, Vec3 center, Vec3 forward, float scale, SkillRanker.Rank rank, Color c1, Color c2, float roll) {
        LodestoneWorldParticleType type = pickCut(rank);

        WorldParticleBuilder.create(type)
                .setTransparencyData(GenericParticleData.create(1.0f, 0.0f).build())
                .setScaleData(GenericParticleData.create(2.4f * scale, 2.4f * scale).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setSpinData(SpinParticleData.create(0, 0).setSpinOffset(roll).build())
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setLifetime(10)
                .spawn(level, center.x, center.y, center.z);

        // tiny forward drift so it “bites” instead of looking glued
        WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                .setTransparencyData(GenericParticleData.create(0.9f, 0.0f).build())
                .setScaleData(GenericParticleData.create(0.35f * scale, 0.0f).build())
                .setColorData(ColorParticleData.create(c2, c1).build())
                .setMotion(forward.x * 0.05, forward.y * 0.02, forward.z * 0.05)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setLifetime(8)
                .spawn(level, center.x, center.y, center.z);
        RankStyle st = styleFor(rank);

// example: spawning one CUT sprite
        WorldParticleBuilder.create(ModParticles.EF_CUT1.get())
                .setTransparencyData(GenericParticleData.create(st.alpha, 0.0f).build())
                .setScaleData(GenericParticleData.create(1.0f * st.size * scale, 0.0f).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setLifetime(st.lifetime)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .spawn(level, center.x, center.y, center.z);


    }

    private static void dashRush(Level level, Vec3 start, Vec3 forward, float scale, SkillRanker.Rank rank, Color c1, Color c2) {
        LodestoneWorldParticleType type = pickRush(rank);

        for (int i = 0; i < 6; i++) {
            Vec3 pos = start.add(forward.scale(i * 0.55));

            WorldParticleBuilder.create(type)
                    .setTransparencyData(GenericParticleData.create(0.9f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(1.8f * scale, 1.8f * scale).build())
                    .setColorData(ColorParticleData.create(c1, c2).build())
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setLifetime(10)
                    .spawn(level, pos.x, pos.y, pos.z);
        }
    }

    private static void blinkPop(Level level, Vec3 center, float scale, SkillRanker.Rank rank, Color c1, Color c2) {
        for (int i = 0; i < 14; i++) {
            double mx = (Math.random() - 0.5) * 0.8;
            double my = (Math.random() - 0.5) * 0.6;
            double mz = (Math.random() - 0.5) * 0.8;

            WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                    .setTransparencyData(GenericParticleData.create(0.9f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(0.35f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(c2, c1).build())
                    .setMotion(mx, my, mz)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setLifetime(10)
                    .spawn(level, center.x, center.y, center.z);
        }
    }

    private static void punch(Level level, Player player, Vec3 center, Vec3 forward, float scale, SkillRanker.Rank rank, Color c1, Color c2) {
        // overload helper if you want player-based randomness later
        punch(level, center, forward, scale, rank, c1, c2);
    }

    private static void punch(Level level, Vec3 center, Vec3 forward, float scale, SkillRanker.Rank rank, Color c1, Color c2) {
        WorldParticleBuilder.create(ModParticles.SOFT_GLOW.get())
                .setTransparencyData(GenericParticleData.create(0.95f, 0.0f).build())
                .setScaleData(GenericParticleData.create(1.2f * scale, 0.0f).build())
                .setColorData(ColorParticleData.create(c1, c2).build())
                .setMotion(forward.x * 0.08, forward.y * 0.03, forward.z * 0.08)
                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                .setLifetime(8)
                .spawn(level, center.x, center.y, center.z);

        for (int i = 0; i < 10; i++) {
            double mx = (Math.random() - 0.5) * 1.1;
            double my = (Math.random() - 0.5) * 0.8;
            double mz = (Math.random() - 0.5) * 1.1;

            WorldParticleBuilder.create(ModParticles.SHARP_SPARK.get())
                    .setTransparencyData(GenericParticleData.create(0.85f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(0.30f * scale, 0.0f).build())
                    .setColorData(ColorParticleData.create(c2, c1).build())
                    .setMotion(mx + forward.x * 0.06, my, mz + forward.z * 0.06)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setLifetime(10)
                    .spawn(level, center.x, center.y, center.z);
        }
    }

    // ---------- Particle type selectors ----------

    private static LodestoneWorldParticleType pickCut(SkillRanker.Rank rank) {
        // You can change mapping however you want; this is a simple “higher rank -> fancier cut”
        int r = Math.min(3, rank.ordinal()); // clamp into 0..3
        return switch (r) {
            case 0 -> ModParticles.EF_CUT1.get();
            case 1 -> ModParticles.EF_CUT2.get();
            case 2 -> ModParticles.EF_CUT3.get();
            default -> ModParticles.EF_CUT4.get();
        };
    }

    private static LodestoneWorldParticleType pickRush(SkillRanker.Rank rank) {
        int r = Math.min(4, rank.ordinal()); // clamp into 0..4
        return switch (r) {
            case 0 -> ModParticles.EF_BR1.get();
            case 1 -> ModParticles.EF_BR2.get();
            case 2 -> ModParticles.EF_BR3.get();
            case 3 -> ModParticles.EF_BR4.get();
            default -> ModParticles.EF_BR5.get();
        };
    }

    // ---------- Aim helper ----------

    private static Vec3 getHitPos(Player player, double dist) {
        HitResult hit = player.pick(dist, 0.0f, false);
        Vec3 loc = hit.getLocation();
        if (loc == null) {
            // fallback: just in front of the player
            return player.getEyePosition().add(player.getLookAngle().normalize().scale(2.2));
        }
        return loc;
    }
    // Put inside StrikeVisuals class

    private record RankStyle(
            float size,          // overall scale multiplier
            float alpha,         // transparency (1 = solid)
            int lifetime,        // ticks
            int corePoints,      // main cut sprites / ribbon density
            int sparkCount,      // extra sparks
            float sparkSpeed,    // spark motion multiplier
            float jitter         // positional jitter (for chaos)
    ) {}

    private static RankStyle styleFor(SkillRanker.Rank rank) {
        // Tune these numbers however you want. This is the “control panel”.
        return switch (rank) {
            case F -> new RankStyle(0.75f, 0.55f, 10, 1,  4, 0.12f, 0.06f);
            case E -> new RankStyle(0.85f, 0.60f, 12, 1,  6, 0.14f, 0.07f);
            case D -> new RankStyle(0.95f, 0.65f, 14, 1,  8, 0.16f, 0.08f);
            case C -> new RankStyle(1.10f, 0.70f, 16, 2, 10, 0.18f, 0.09f);
            case B -> new RankStyle(1.30f, 0.78f, 18, 2, 14, 0.22f, 0.10f);
            case A -> new RankStyle(1.55f, 0.85f, 22, 3, 18, 0.28f, 0.12f);
            case S -> new RankStyle(1.85f, 0.92f, 26, 4, 24, 0.35f, 0.14f);
            // If you have SS / SSS in your enum, add them. If not, just keep S as max.
            default -> new RankStyle(2.10f, 0.98f, 30, 5, 30, 0.45f, 0.16f);
        };
    }

    // Tiny random helper
    private static double r(Player p) {
        return p.getRandom().nextDouble();
    }

}
