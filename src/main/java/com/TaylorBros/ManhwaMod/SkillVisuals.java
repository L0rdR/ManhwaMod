package com.TaylorBros.ManhwaMod;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.phys.Vec3;

public class SkillVisuals {

    public static void play(Player player, String skillName, String fullRecipeData) {
        if (!(player.level() instanceof ServerLevel level)) return;

        // 1. PARSE DATA
        String[] parts = fullRecipeData.split(":");
        String shapeName = parts.length > 0 ? parts[0].trim().toUpperCase() : "SINGLE";

        SkillRanker.Rank rank = SkillRanker.getRank(fullRecipeData);
        float rankScale = 1.0f + (rank.ordinal() * 0.5f);

        Vec3 pos = player.position().add(0, 1.5, 0);
        Vec3 look = player.getLookAngle();

        // 2. PLACEHOLDER PARTICLES (Clean Vanilla Options)
        // We will replace these with Lodestone VFX builders later.
        ParticleOptions mainParticle = ParticleTypes.ELECTRIC_SPARK; // Default sharp energy
        ParticleOptions coreParticle = ParticleTypes.END_ROD; // Bright white center
        ParticleOptions heavyDebris = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());

        // Select particle based on Rank to keep it looking okay for now
        if (rank.ordinal() >= SkillRanker.Rank.A.ordinal()) mainParticle = ParticleTypes.SONIC_BOOM;
        else if (rank.ordinal() >= SkillRanker.Rank.C.ordinal()) mainParticle = ParticleTypes.DRAGON_BREATH;

        // 3. SOUND & SCREEN SHAKE
        playRankSound(level, player, rank);
        if (rank.ordinal() >= SkillRanker.Rank.A.ordinal()) {
            float shake = rank.ordinal() >= SkillRanker.Rank.S.ordinal() ? 1.5f : 0.5f;
            int radius = rank.ordinal() >= SkillRanker.Rank.S.ordinal() ? 900 : 100;
            for (Player p : level.players()) {
                if (p.distanceToSqr(player) < radius) {
                    Messages.sendToPlayer(new PacketScreenShake(shake, 20), (ServerPlayer) p);
                }
            }
        }

        // 4. GEOMETRY (Using the placeholders)
        if (shapeName.contains("SLASH")) {
            boolean vert = shapeName.contains("VERT");
            spawnFlyingArc(level, pos, look, mainParticle, 3.5 * rankScale, vert);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x + look.x, pos.y + look.y, pos.z + look.z, 1, look.x, look.y, look.z, 0);
        }
        else if (shapeName.contains("BEAM") || shapeName.contains("RAY")) {
            spawnDenseBeam(level, pos, look, coreParticle, 20.0, 0.2 * rankScale);
            spawnHelix(level, pos, look, mainParticle, 15.0, 0.8 * rankScale);
            spawnShockwave(level, player.position(), mainParticle, 2.0 * rankScale);
        }
        else if (shapeName.contains("BALL") || shapeName.contains("FLARE") || shapeName.contains("BOOMERANG")) {
            spawnImplosion(level, pos.add(look), mainParticle, 1.5 * rankScale);
            spawnSphere(level, pos.add(look), coreParticle, 0.4 * rankScale, 15);
            spawnBurst(level, pos, ParticleTypes.CLOUD, 5, 0.1);
        }
        else if (shapeName.contains("AOE") || shapeName.contains("IMPACT") || shapeName.contains("NOVA")) {
            spawnShockwave(level, player.position(), mainParticle, 5.0 * rankScale);
            spawnPillar(level, player.position(), coreParticle, 4.0 * rankScale, 20);
            if (rank.ordinal() >= SkillRanker.Rank.B.ordinal()) {
                spawnDebris(level, player.position(), heavyDebris, 3.0 * rankScale);
            }
        }
        else {
            spawnBurst(level, pos.add(look), mainParticle, (int)(20 * rankScale), 0.3);
        }
    }

    // --- GEOMETRY ENGINES (Kept the physics, removed the colors) ---

    private static void spawnFlyingArc(ServerLevel level, Vec3 start, Vec3 dir, ParticleOptions p, double radius, boolean vertical) {
        Vec3 right = dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(dir).normalize();
        int points = 30;
        for (int i = 0; i <= points; i++) {
            double t = (double)i / points;
            double angle = (t - 0.5) * Math.PI * 0.8;
            Vec3 offset = vertical
                    ? dir.scale(Math.cos(angle) * radius * 0.2).add(up.scale(Math.sin(angle) * radius))
                    : dir.scale(Math.cos(angle) * radius * 0.2).add(right.scale(Math.sin(angle) * radius));
            level.sendParticles(p, start.x + offset.x, start.y + offset.y, start.z + offset.z, 0, dir.x, dir.y, dir.z, 0.5);
        }
    }

    private static void spawnDenseBeam(ServerLevel level, Vec3 start, Vec3 dir, ParticleOptions p, double length, double width) {
        int points = (int)(length * 4);
        for(int i=0; i<points; i++) {
            Vec3 pos = start.add(dir.scale(i * 0.25));
            level.sendParticles(p, pos.x, pos.y, pos.z, 1, width, width, width, 0);
        }
    }

    private static void spawnShockwave(ServerLevel level, Vec3 center, ParticleOptions p, double radius) {
        int count = (int)(radius * 20);
        for(int i=0; i<count; i++) {
            double angle = 2 * Math.PI * i / count;
            level.sendParticles(p, center.x, center.y + 0.2, center.z, 0, Math.cos(angle), 0, Math.sin(angle), 0.5);
        }
    }

    private static void spawnImplosion(ServerLevel level, Vec3 center, ParticleOptions p, double radius) {
        int count = 20;
        for(int i=0; i<count; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.acos(2 * Math.random() - 1);
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);
            level.sendParticles(p, center.x+(x*radius), center.y+(y*radius), center.z+(z*radius), 0, -x, -y, -z, 0.2);
        }
    }

    private static void spawnDebris(ServerLevel level, Vec3 center, ParticleOptions p, double radius) {
        int count = 15;
        for(int i=0; i<count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double r = Math.random() * radius;
            level.sendParticles(p, center.x+Math.cos(angle)*r, center.y+0.5, center.z+Math.sin(angle)*r, 1, 0, 0.3, 0, 0.1);
        }
    }

    private static void spawnPillar(ServerLevel level, Vec3 center, ParticleOptions p, double height, int density) {
        for(int i=0; i<density; i++) {
            double y = Math.random() * height;
            double angle = Math.random() * 2 * Math.PI;
            level.sendParticles(p, center.x + Math.cos(angle)*0.5, center.y + y, center.z + Math.sin(angle)*0.5, 1, 0, 0.1, 0, 0);
        }
    }

    private static void spawnHelix(ServerLevel level, Vec3 start, Vec3 dir, ParticleOptions p, double length, double radius) {
        dir = dir.normalize();
        Vec3 right = dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(dir).normalize();
        int steps = (int)(length * 5);
        for (int i = 0; i < steps; i++) {
            double dist = i * 0.2;
            double angle = dist * 2.0;
            Vec3 core = start.add(dir.scale(dist));
            double offX = Math.cos(angle) * radius;
            double offY = Math.sin(angle) * radius;
            Vec3 pos1 = core.add(right.scale(offX)).add(up.scale(offY));
            Vec3 pos2 = core.add(right.scale(-offX)).add(up.scale(-offY));
            level.sendParticles(p, pos1.x, pos1.y, pos1.z, 1, 0, 0, 0, 0);
            level.sendParticles(p, pos2.x, pos2.y, pos2.z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnSphere(ServerLevel level, Vec3 center, ParticleOptions p, double radius, int count) {
        for(int i=0; i<count; i++) {
            double u = Math.random(); double v = Math.random();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);
            level.sendParticles(p, center.x+x*radius, center.y+y*radius, center.z+z*radius, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnBurst(ServerLevel level, Vec3 pos, ParticleOptions p, int count, double speed) {
        level.sendParticles(p, pos.x, pos.y, pos.z, count, speed, speed, speed, 0.05);
    }

    private static void playRankSound(Level level, Player player, SkillRanker.Rank rank) {
        if(rank.ordinal() >= SkillRanker.Rank.S.ordinal()) {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.5f);
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.0f);
        } else if (rank.ordinal() >= SkillRanker.Rank.B.ordinal()) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }
}