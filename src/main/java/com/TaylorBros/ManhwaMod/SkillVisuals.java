package com.TaylorBros.ManhwaMod;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SkillVisuals {

    public static void play(Player player, String skillName, String fullRecipeData) {
        // 1. Safety Check: Only run on Server
        if (!(player.level() instanceof ServerLevel level)) return;

        SkillRanker.Rank rank = SkillRanker.getRank(fullRecipeData);
        Vec3 pos = player.position().add(0, 1.5, 0);
        Vec3 look = player.getLookAngle();

        // 2. Play Sound
        playRankSound(level, player, rank);

        // 3. Screen Shake Logic (The Impact)
        if (rank == SkillRanker.Rank.A) {
            // Mild Shake for A-Rank
            Messages.sendToPlayer(new PacketScreenShake(0.5f, 10), (ServerPlayer) player);
        }
        else if (rank == SkillRanker.Rank.S || rank == SkillRanker.Rank.SS) {
            // VIOLENT Shake for God-Tier (Everyone nearby feels it)
            for (Player p : level.players()) {
                if (p.distanceToSqr(player) < 900) { // 30 block radius
                    Messages.sendToPlayer(new PacketScreenShake(1.5f, 25), (ServerPlayer) p);
                }
            }
        }

        // 4. Geometry Particles
        switch (rank) {
            case E:
            case D:
                spawnBurst(level, pos, ParticleTypes.CRIT, 10, 0.2);
                break;

            case C:
            case B:
                // Magic Ring
                spawnRing(level, player.position().add(0, 0.2, 0), ParticleTypes.ENCHANTED_HIT, 1.5, 30);
                spawnBurst(level, pos.add(look), ParticleTypes.FLAME, 15, 0.2);
                break;

            case A:
                // DNA Spiral Beam
                spawnHelix(level, pos, look, ParticleTypes.DRAGON_BREATH, 5.0, 0.8);
                spawnRing(level, player.position().add(0, 0.5, 0), ParticleTypes.SOUL_FIRE_FLAME, 2.0, 40);
                break;

            case S:
            case SS:
                // God-Tier Implosion
                spawnSphere(level, pos, ParticleTypes.SONIC_BOOM, 2.5, 40);
                spawnHelix(level, pos, look, ParticleTypes.END_ROD, 8.0, 1.2);
                spawnBurst(level, pos, ParticleTypes.EXPLOSION_EMITTER, 1, 0);
                break;
        }
    }

    // --- MATH ENGINES (Server Side) ---

    private static void spawnBurst(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, speed, speed, speed, 0.1);
    }

    private static void spawnRing(ServerLevel level, Vec3 center, ParticleOptions particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            level.sendParticles(particle, center.x + x, center.y, center.z + z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnSphere(ServerLevel level, Vec3 center, ParticleOptions particle, double radius, int density) {
        for (int i = 0; i < density; i++) {
            double u = Math.random();
            double v = Math.random();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double x = center.x + (radius * Math.sin(phi) * Math.cos(theta));
            double y = center.y + (radius * Math.sin(phi) * Math.sin(theta));
            double z = center.z + (radius * Math.cos(phi));
            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnHelix(ServerLevel level, Vec3 start, Vec3 dir, ParticleOptions particle, double length, double radius) {
        dir = dir.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.95) up = new Vec3(1, 0, 0);

        Vec3 right = dir.cross(up).normalize();
        Vec3 finalUp = right.cross(dir).normalize();

        int particlesPerBlock = 8;
        int totalSteps = (int) (length * particlesPerBlock);

        for (int i = 0; i < totalSteps; i++) {
            double progress = (double) i / particlesPerBlock;
            Vec3 centerPoint = start.add(dir.scale(progress));
            double angle = progress * 2.5;

            double offsetX = Math.cos(angle) * radius;
            double offsetY = Math.sin(angle) * radius;

            // Strand 1
            Vec3 p1 = centerPoint.add(right.scale(offsetX)).add(finalUp.scale(offsetY));
            level.sendParticles(particle, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0);

            // Strand 2
            Vec3 p2 = centerPoint.add(right.scale(-offsetX)).add(finalUp.scale(-offsetY));
            level.sendParticles(particle, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0);
        }
    }

    private static void playRankSound(Level level, Player player, SkillRanker.Rank rank) {
        switch (rank) {
            case E: case D:
                level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0f, 1.5f);
                break;
            case C: case B:
                level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
                break;
            case A:
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;
            case S: case SS:
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 0.8f);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.0f);
                break;
        }
    }
}