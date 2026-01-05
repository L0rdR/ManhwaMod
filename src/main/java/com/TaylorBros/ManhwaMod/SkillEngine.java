package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.server.TickTask;

public class SkillEngine {

    public static void execute(ServerPlayer player, int skillId) {
        String recipe = player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId);
        int cost = player.getPersistentData().getInt("manhwamod.skill_cost_" + skillId);
        if (recipe == null || recipe.isEmpty()) return;

        try {
            String[] parts = recipe.split(":");
            if (parts.length < 3) return;

            SkillTags.Shape shape = SkillTags.Shape.valueOf(parts[0].toUpperCase().trim().replace(" ", "_"));
            SkillTags.Element element = SkillTags.Element.valueOf(parts[1].toUpperCase().trim());
            SkillTags.Modifier modifier = SkillTags.Modifier.valueOf(parts[2].toUpperCase().trim());

            // --- COOLDOWN SYSTEM ---
            long currentTime = player.level().getGameTime();
            String cdKey = "manhwamod.cd_timer_" + skillId;
            if (currentTime < player.getPersistentData().getLong(cdKey)) {
                player.displayClientMessage(Component.literal("§cArt Cooldown!"), true);
                return;
            }
            player.getPersistentData().putLong(cdKey, currentTime + getBaseShapeCooldown(shape) + (int)(cost * 0.2f));

            // --- STYLING & MULTIPLIERS ---
            float multi = 1.0f + (cost / 100.0f);
            SimpleParticleType p1 = getElementParticle(element);
            SimpleParticleType p2 = getModifierParticle(modifier);
            playSkillSounds(player, element, modifier);

            // Activation Aura
            player.serverLevel().sendParticles(p1, player.getX(), player.getY(), player.getZ(), 15, 0.4, 0.1, 0.4, 0.05);

            // --- SHAPE ROUTER ---
            switch (shape) {
                case BALL -> runBall(player, p1, p2, modifier, multi);
                case RAY -> runRay(player, p1, p2, modifier, multi);
                case BEAM -> runBeam(player, p1, multi, modifier);
                case SINGLE -> runSingle(player, p1, multi, modifier);
                case PUNCH -> runPunch(player, p1, p2, modifier, multi);
                case DASH -> runDash(player, p1, modifier, multi);
                case SLASH, VERT_SLASH, HORIZ_SLASH -> runSlash(player, p1, 0, shape == SkillTags.Shape.VERT_SLASH, modifier, multi);
                case WALL -> runInstantWall(player, p1, cost, multi);
                case SPIKES -> runSpikes(player, p1, p2, modifier, multi, cost);
                case BARRAGE -> runBarrage(player, p1, p2, modifier, multi, cost);
                case BARRAGE_PUNCH -> runBarragePunch(player, p1, p2, modifier, multi, cost);
                case SLASH_BARRAGE -> runSlashBarrage(player, p1, modifier, multi, cost);
                case RAIN -> runRain(player, p1, p2, modifier, multi, cost);
                case BLINK_STRIKE -> runBlinkStrike(player, p1, modifier, multi);
                case FLARE -> runProjectileFlare(player, p1, p2, modifier, multi, cost);
                case IMPACT_BURST -> runImpactBurst(player, p1, p2, modifier, multi, cost);
                case AOE -> runAOE(player, p1, p2, modifier, multi, cost);
                case CONE -> runCone(player, p1, modifier, multi);
                case BOOMERANG -> runBoomerang(player, p1, p2, modifier, multi, cost);
                default -> runSingle(player, p1, multi, modifier); // Safer fallback
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==================================================
    // SPECIALIZED SHAPE LOGIC
    // ==================================================

    private static void runProjectileFlare(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        int travelTime = 12;

        for (int i = 0; i < travelTime; i++) {
            int delay = i;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                Vec3 pos = start.add(look.scale(delay * 1.5));
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 5, 0.1, 0.1, 0.1, 0.05);

                if (delay == travelTime - 1) {
                    p.serverLevel().sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                    p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 20, 1.0, 1.0, 1.0, 0.05);
                    damageArea(p, pos, 4.5 + (cost/40f), 14.0f * multi, m);
                    p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }));
        }
    }

    private static void runAOE(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        double radius = 4.0 + (cost / 40.0);
        Vec3 center = p.position(); // Lock the position to where the player cast it

        // This loop creates the "Lingering" effect for 80 ticks (4 seconds)
        for (int t = 0; t < 80; t += 5) {
            final int delay = t;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;

                // 1. Visuals: Draw the boundary of the domain
                for (int degree = 0; degree < 360; degree += 20) {
                    double rad = Math.toRadians(degree);
                    double x = center.x + Math.cos(rad) * radius;
                    double z = center.z + Math.sin(rad) * radius;
                    p.serverLevel().sendParticles(p1, x, center.y + 0.2, z, 1, 0, 0, 0, 0);
                }

                // 2. Visuals: Random "steam/energy" inside the circle
                p.serverLevel().sendParticles(p2, center.x, center.y + 0.5, center.z, 5, radius/2, 0.5, radius/2, 0.02);

                // 3. Logic: Damage everyone inside the zone every half-second
                p.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 2, radius), e -> e != p).forEach(targ -> {
                    targ.hurt(p.damageSources().magic(), 1.5f * multi); // Constant pressure damage
                    applyModifier(targ, m, p); // Constant status application
                });

                // 4. Sound: Low humming sound
                p.level().playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.5f);
            }));
        }
    }

    private static void runBoomerang(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        int duration = 30; // Total travel time (1.5 seconds)
        Vec3 startPos = p.getEyePosition();
        Vec3 forward = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize(); // Perpendicular for the arc

        for (int i = 0; i <= duration; i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + step, () -> {
                if (p.isRemoved()) return;

                // 1. PATH LOGIC (The Arc)
                double progress = (double) step / duration;
                // Forward: Goes out 10 blocks and back to 0 (using Sine)
                double distForward = Math.sin(progress * Math.PI) * 10.0;
                // Side: Curves out 5 blocks (using Sine)
                double distSide = Math.sin(progress * Math.PI) * 5.0;

                // Update current position based on the arc math
                Vec3 currentPos = startPos.add(forward.scale(distForward)).add(side.scale(distSide));

                // 2. VISUALS: The Spinning Blade
                // We draw 4 particles in a cross that rotates over time
                for (int deg = 0; deg < 360; deg += 90) {
                    double rad = Math.toRadians(deg + (step * 45)); // Rotation speed
                    Vec3 spin = currentPos.add(new Vec3(Math.cos(rad) * 0.6, 0, Math.sin(rad) * 0.6));

                    p.serverLevel().sendParticles(p1, spin.x, spin.y, spin.z, 1, 0, 0, 0, 0);
                }

                // Core trail and modifier particles
                p.serverLevel().sendParticles(p2, currentPos.x, currentPos.y, currentPos.z, 2, 0.1, 0.1, 0.1, 0.02);

                // 3. LOGIC: Hitbox
                damageArea(p, currentPos, 1.8, 6.0f * multi, m);

                // 4. SOUND: Whirring effect
                if (step % 3 == 0) {
                    p.level().playSound(null, currentPos.x, currentPos.y, currentPos.z,
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.2f + (float)progress);
                }
            }));
        }
    }

    private static void runBeam(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m) {
        for (int t = 0; t < 20; t++) {
            final int tick = t;
            p.server.tell(new TickTask(p.server.getTickCount() + tick, () -> {
                if (p.isRemoved()) return;
                // Fires a 15-block beam every tick
                Vec3 start = p.getEyePosition();
                Vec3 look = p.getLookAngle();
                for (double d = 0; d < 15; d += 1.0) {
                    Vec3 pt = start.add(look.scale(d));
                    p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 2, 0.1, 0.1, 0.1, 0.02);
                    damageArea(p, pt, 1.2, 1.5f * multi, m); // Lower damage per tick since it hits 20 times
                }
            }));
        }
    }

    private static void runBall(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        int lifetime = 30; // Lasts 1.5 seconds

        for (int i = 0; i < lifetime; i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + step, () -> {
                if (p.isRemoved()) return;

                // Slow travel speed: 0.5 blocks per tick
                Vec3 pos = start.add(look.scale(step * 0.5));

                // Visuals: Dense core with rotating outer shell
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 15, 0.2, 0.2, 0.2, 0.05);
                p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 2, 0.5, 0.5, 0.5, 0.1);

                // GRAVITY WELL: Pull entities within 5 blocks toward the center of the ball
                p.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(5.0), e -> e != p).forEach(t -> {
                    Vec3 pull = pos.subtract(t.position()).normalize().scale(0.2);
                    t.setDeltaMovement(t.getDeltaMovement().add(pull));
                    t.hurtMarked = true;

                    // Constant tick damage while stuck in the gravity well
                    t.hurt(p.damageSources().magic(), 1.0f * multi);
                });

                // Final explosion at the end of its life
                if (step == lifetime - 1) {
                    p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 30, 1.5, 1.5, 1.5, 0.1);
                    damageArea(p, pos, 4.0, 10.0f * multi, m);
                    p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.5f);
                }
            }));
        }
    }

    private static void runBarrage(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        int totalVolleys = 4 + (cost / 30); // How many times each arc fires
        Vec3 look = p.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 up = look.cross(right).normalize();

        // 1. Create the 6 Arc Points around the player
        Vec3[] arcPoints = new Vec3[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(-60 + (i * 24)); // Spreads points in a 120-degree arc
            arcPoints[i] = p.getEyePosition().add(right.scale(Math.cos(angle) * 1.5)).add(up.scale(Math.sin(angle) * 1.5));
        }

        // 2. Fire the Lazers from each point sequentially
        for (int volley = 0; volley < totalVolleys; volley++) {
            final int v = volley;
            p.server.tell(new TickTask(p.server.getTickCount() + (v * 5), () -> {
                if (p.isRemoved()) return;

                for (Vec3 startPos : arcPoints) {
                    // Draw a "Little Lazer" (5 blocks long)
                    for (double d = 0; d < 6; d += 0.5) {
                        Vec3 beamPt = startPos.add(look.scale(d));
                        p.serverLevel().sendParticles(p1, beamPt.x, beamPt.y, beamPt.z, 1, 0, 0, 0, 0.01);

                        // Damage logic for each mini-beam
                        damageArea(p, beamPt, 0.8, 2.0f * multi, m);
                    }

                    // High-pitched "pew" sound for energy bolts
                    p.level().playSound(null, startPos.x, startPos.y, startPos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.5f + (v * 0.1f));
                }

                // Modifier particles at the end of the lazers
                Vec3 impactGlow = p.getEyePosition().add(look.scale(6));
                p.serverLevel().sendParticles(p2, impactGlow.x, impactGlow.y, impactGlow.z, 5, 0.5, 0.5, 0.5, 0.05);
            }));
        }
    }

    private static void runInstantWall(ServerPlayer player, SimpleParticleType p1, int cost, float multi) {
        Vec3 forward = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 center = player.position().add(forward.scale(2.5));
        int width = 2 + (cost / 50);
        player.serverLevel().sendParticles(p1, center.x, center.y + 1, center.z, 60, width, 1.5, width, 0.05);
        player.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(width, 2, width), e -> e != player).forEach(t -> {
            Vec3 push = t.position().subtract(player.position()).normalize().add(0, 0.2, 0).scale(1.5);
            t.setDeltaMovement(push);
            t.hurtMarked = true;
            t.hurt(player.damageSources().magic(), 4.0f * multi);
        });
    }

    private static void runSpikes(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        int count = 6 + (cost / 15); // Increased count for better range
        Vec3 direction = p.getLookAngle().multiply(1, 0, 1).normalize(); // Horizontal direction

        for (int i = 1; i <= count; i++) {
            final int step = i;
            // Delay each spike by 1 or 2 ticks to create the ripple effect
            p.server.tell(new TickTask(p.server.getTickCount() + (step * 2), () -> {
                if (p.isRemoved()) return;

                Vec3 pos = p.position().add(direction.scale(step * 1.2)); // Space them out 1.2 blocks

                // 1. Visuals: Tall pillar of particles
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 20, 0.2, 1.5, 0.2, 0.1);
                p.serverLevel().sendParticles(p2, pos.x, pos.y + 0.5, pos.z, 5, 0.3, 0.3, 0.3, 0.05);

                // 2. Sound: Deep "stone/impact" sound for each spike
                p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, 0.6f, 0.8f + (step * 0.05f));
                p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.3f, 1.5f);

                // 3. Logic: Damage and Knockup
                p.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(1.5, 2.0, 1.5), e -> e != p).forEach(t -> {
                    t.hurt(p.damageSources().magic(), 6.0f * multi);
                    // Throw enemies slightly UP as the spike hits them
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 0.45, 0));
                    t.hurtMarked = true;
                    applyModifier(t, m, p);
                });
            }));
        }
    }

    private static void runRain(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        // 1. Find the impact center on the ground
        BlockHitResult hit = p.level().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(25)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 center = hit.getLocation();
        double cloudHeight = 12.0;

        // 2. SPAWN THE CLOUD (Circular disk 12 blocks up)
        for (int degree = 0; degree < 360; degree += 15) {
            double rad = Math.toRadians(degree);
            for (double r = 0; r < 5.0; r += 1.5) { // Creates a thick 5-block radius cloud
                double cloudX = center.x + (Math.cos(rad) * r);
                double cloudZ = center.z + (Math.sin(rad) * r);
                // Mix of Smoke for mass and p1 for elemental color
                p.serverLevel().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cloudX, center.y + cloudHeight, cloudZ, 2, 0.2, 0.1, 0.2, 0.02);
                p.serverLevel().sendParticles(p1, cloudX, center.y + cloudHeight, cloudZ, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // 3. SOUND: Ominous thunder/charge sound at the cloud
        p.level().playSound(null, center.x, center.y + cloudHeight, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 0.5f);

        // 4. SEQUENTIAL STRIKES
        int totalBolts = 8 + (cost / 15);
        for (int i = 0; i < totalBolts; i++) {
            final int delay = 10 + (i * 3); // Starts 10 ticks after cloud appears
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;

                // Random offset within the cloud radius
                double offsetX = (p.getRandom().nextDouble() - 0.5) * 8;
                double offsetZ = (p.getRandom().nextDouble() - 0.5) * 8;
                Vec3 strikePos = center.add(offsetX, 0, offsetZ);

                // Visual: Pillar falling FROM the cloud
                for (double h = 0; h < cloudHeight; h += 1.0) {
                    p.serverLevel().sendParticles(p1, strikePos.x, strikePos.y + h, strikePos.z, 3, 0.05, 0.5, 0.05, 0.02);
                }

                p.serverLevel().sendParticles(ParticleTypes.FLASH, strikePos.x, strikePos.y, strikePos.z, 1, 0, 0, 0, 0);
                p.serverLevel().sendParticles(p2, strikePos.x, strikePos.y, strikePos.z, 8, 0.3, 0.3, 0.3, 0.05);

                damageArea(p, strikePos, 2.5, 5.0f * multi, m);
                p.level().playSound(null, strikePos.x, strikePos.y, strikePos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.3f, 1.8f);
            }));
        }
    }

    private static void runCone(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi) {
        // Fired over 15 ticks (0.75 seconds)
        for (int t = 0; t < 15; t++) {
            final int delay = t;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;

                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getLookAngle();

                // Generate the "V" shape particles
                for (int angle = -25; angle <= 25; angle += 5) {
                    Vec3 dir = Vec3.directionFromRotation(p.getXRot(), p.getYRot() + angle);
                    for (double d = 1; d < 7; d += 1.5) {
                        Vec3 pt = eye.add(dir.scale(d));
                        // Particles get bigger/more scattered at the end of the cone
                        p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 2, d * 0.1, d * 0.1, d * 0.1, 0.02);
                        damageArea(p, pt, 1.2, 1.2f * multi, m);
                    }
                }
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.4f, 1.2f);
            }));
        }
    }

    private static void runBlinkStrike(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier mod, float multi) {
        Vec3 dest = p.getEyePosition().add(p.getLookAngle().scale(10));
        BlockHitResult hit = p.level().clip(new ClipContext(p.getEyePosition(), dest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 finalPos = hit.getLocation().subtract(p.getLookAngle().scale(1.0));
        p.serverLevel().sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 5, 0.2, 0.2, 0.2, 0);
        p.teleportTo(finalPos.x, finalPos.y, finalPos.z);
        damageArea(p, finalPos, 3.5, 12.0f * multi, mod);
        p.serverLevel().sendParticles(p1, finalPos.x, finalPos.y + 1, finalPos.z, 20, 0.5, 0.5, 0.5, 0.1);
    }

    private static void runPunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi) {
        Vec3 pt = p.getEyePosition().add(p.getLookAngle().scale(1.5));
        p.serverLevel().sendParticles(ParticleTypes.EXPLOSION, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        p.serverLevel().sendParticles(p2, pt.x, pt.y, pt.z, 10, 0.3, 0.3, 0.3, 0.05);
        damageArea(p, pt, 2.0, 9.0f * multi, m);
    }

    private static void runDash(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi) {
        // 1. Give brief Invincibility (Resistance 5 = 100% damage reduction)
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 5, false, false));

        // 2. Launch the player
        Vec3 dashDir = p.getLookAngle().normalize();
        p.setDeltaMovement(dashDir.scale(2.2)); // Increased speed
        p.hurtMarked = true;

        // 3. Create a trail of "Afterimages" over the next 5 ticks
        for (int i = 0; i < 6; i++) {
            final int delay = i;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;

                // Spawn a cloud of element particles at the player's trail position
                p.serverLevel().sendParticles(p1, p.getX(), p.getY() + 1, p.getZ(), 10, 0.2, 0.5, 0.2, 0.02);
                p.serverLevel().sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);

                // Damage anyone the player "collides" with during the dash
                damageArea(p, p.position(), 2.5, 4.0f * multi, m);
            }));
        }

        // 4. Sound: High-speed sonic boom
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.8f);
    }

    private static void runSlash(ServerPlayer p, SimpleParticleType p1, float yawOff, boolean vert, SkillTags.Modifier m, float multi) {
        int step = 0;
        for (int i = -35; i <= 35; i += 10) {
            final int angleOffset = i; // Create a FINAL copy of i
            final int delay = step;    // Create a FINAL copy of step

            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;

                // Use the final 'angleOffset' instead of 'i'
                float currentYaw = vert ? p.getYRot() + yawOff : p.getYRot() + angleOffset + yawOff;
                float currentPitch = vert ? p.getXRot() + angleOffset : p.getXRot();

                Vec3 dir = Vec3.directionFromRotation(currentPitch, currentYaw);

                for (double d = 1.5; d <= 5.0; d += 0.5) {
                    Vec3 pt = p.getEyePosition().add(dir.scale(d));
                    p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 2, 0.05, 0.05, 0.05, 0.02);
                    damageArea(p, pt, 1.2, 7.0f * multi, m);
                }

                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.2f + (delay * 0.1f));
            }));
            step++;
        }
    }

    private static void runSlashBarrage(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, int cost) {
        int totalStrikes = 5 + (cost / 25);
        int tickDelay = 4; // Fast pace

        for (int i = 0; i < totalStrikes; i++) {
            final int strikeNum = i;
            final int currentDelay = i * tickDelay;

            p.server.tell(new TickTask(p.server.getTickCount() + currentDelay, () -> {
                if (p.isRemoved()) return;

                // Alternate: Even numbers are Horizontal, Odd are Vertical
                boolean isVertical = (strikeNum % 2 != 0);

                // We call our new runSlash logic directly!
                // yawOff 0, multi slightly reduced per hit since it's a barrage (0.6f)
                runSlash(p, p1, 0, isVertical, m, multi * 0.7f);

                // Extra visual: A flash at the player's position for each swing
                p.serverLevel().sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
            }));
        }
    }

    private static void runBarragePunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        int punches = 12 + (cost / 10);
        for (int i = 0; i < punches; i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + (i * 2), () -> {
                if (p.isRemoved()) return;

                // Alternate left and right offset for "two-handed" feel
                double sideOffset = (step % 2 == 0) ? 0.4 : -0.4;
                Vec3 look = p.getLookAngle();
                Vec3 right = new Vec3(-look.z, 0, look.x).normalize().scale(sideOffset);

                // Punch reaches 3.5 blocks out
                Vec3 punchPos = p.getEyePosition().add(look.scale(2.5)).add(right);

                // Visuals: Impact "Poof" and Element particles
                p.serverLevel().sendParticles(ParticleTypes.EXPLOSION, punchPos.x, punchPos.y, punchPos.z, 1, 0, 0, 0, 0);
                p.serverLevel().sendParticles(p1, punchPos.x, punchPos.y, punchPos.z, 5, 0.1, 0.1, 0.1, 0.02);

                // Sound: Rapid "thuds"
                p.level().playSound(null, punchPos.x, punchPos.y, punchPos.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.5f, 1.2f + (p.getRandom().nextFloat() * 0.4f));

                // Logic: Damage + "Vacuum" effect (pulls them slightly in so they stay in range)
                p.level().getEntitiesOfClass(LivingEntity.class, new AABB(punchPos, punchPos).inflate(1.5), e -> e != p).forEach(t -> {
                    t.hurt(p.damageSources().magic(), 3.0f * multi);
                    Vec3 pull = p.position().subtract(t.position()).normalize().scale(0.15);
                    t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.1, pull.z));
                    t.hurtMarked = true;
                    applyModifier(t, m, p);
                });
            }));
        }
    }

    private static void runRay(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double maxDist = 40.0; // Very long range

        // Find the actual end point (either 40 blocks away or the block it hits)
        BlockHitResult hit = p.level().clip(new ClipContext(start, start.add(look.scale(maxDist)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double actualDist = hit.getLocation().distanceTo(start);

        // Visual: Single thin line of particles
        for (double d = 0; d < actualDist; d += 0.5) {
            Vec3 pt = start.add(look.scale(d));
            p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }

        // Impact Visual and Damage
        Vec3 end = hit.getLocation();
        p.serverLevel().sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        damageArea(p, end, 1.5, 12.0f * multi, m);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 2.0f);
    }

    private static void runSingle(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        // Travels 20 blocks in 10 ticks (very fast)
        for (int i = 0; i < 10; i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + step, () -> {
                Vec3 pos = start.add(look.scale(step * 2.0));
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 5, 0.05, 0.05, 0.05, 0.02);
                damageArea(p, pos, 1.0, 6.0f * multi, m);
            }));
        }
    }

    private static void runImpactBurst(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost) {
        double maxRadius = 4.0 + (cost / 30.0);

        // Expand the ring over 5 ticks (0.25 seconds)
        for (int r = 1; r <= 5; r++) {
            final double currentRadius = (maxRadius / 5.0) * r;
            final int delay = r;

            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;

                // Create a ring of particles at the current expanding radius
                for (int degree = 0; degree < 360; degree += 10) {
                    double rad = Math.toRadians(degree);
                    double x = p.getX() + Math.cos(rad) * currentRadius;
                    double z = p.getZ() + Math.sin(rad) * currentRadius;

                    p.serverLevel().sendParticles(p1, x, p.getY() + 0.5, z, 1, 0, 0.1, 0, 0.01);
                    if (degree % 40 == 0) {
                        p.serverLevel().sendParticles(p2, x, p.getY() + 0.5, z, 1, 0, 0, 0, 0.01);
                    }
                }

                // Damage and knockback enemies caught in the expanding wave
                p.level().getEntitiesOfClass(LivingEntity.class, new AABB(p.position(), p.position()).inflate(currentRadius + 1.0), e -> e != p).forEach(t -> {
                    t.hurt(p.damageSources().magic(), 8.0f * multi);
                    // Physical knockback away from center
                    Vec3 push = t.position().subtract(p.position()).normalize().scale(0.8);
                    t.setDeltaMovement(t.getDeltaMovement().add(push.x, 0.3, push.z));
                    t.hurtMarked = true;
                    applyModifier(t, m, p);
                });

                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 0.5f + (delay * 0.2f));
            }));
        }
    }

    // ==================================================
    // UTILITIES & HELPERS
    // ==================================================

    private static void damageArea(ServerPlayer player, Vec3 pos, double range, float dmg, SkillTags.Modifier mod) {
        player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(range), e -> e != player).forEach(t -> {
            t.hurt(player.damageSources().magic(), dmg);
            applyModifier(t, mod, player);
        });
    }

    private static void applyModifier(LivingEntity t, SkillTags.Modifier mod, ServerPlayer p) {
        switch (mod) {
            case EXPLODE -> t.level().explode(null, t.getX(), t.getY(), t.getZ(), 1.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            case STUN -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10));
            case LIFESTEAL -> p.heal(1.0f);
            case WEAKEN -> {
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            case BOUNCE -> {
                Vec3 knockback = t.position().subtract(p.position()).normalize().add(0, 0.5, 0).scale(1.2);
                t.setDeltaMovement(knockback);
                t.hurtMarked = true;
            }
        }
    }

    private static void playSkillSounds(ServerPlayer p, SkillTags.Element e, SkillTags.Modifier m) {
        net.minecraft.sounds.SoundEvent elementSound = switch(e) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case ICE -> SoundEvents.SNOW_GOLEM_DEATH;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case VOID -> SoundEvents.ENDERMAN_TELEPORT;
            case FORCE -> SoundEvents.WARDEN_ATTACK_IMPACT;
            default -> SoundEvents.PLAYER_ATTACK_SWEEP;
        };
        net.minecraft.sounds.SoundEvent modSound = switch(m) {
            case EXPLODE -> SoundEvents.GENERIC_EXPLODE;
            case STUN -> SoundEvents.CONDUIT_ATTACK_TARGET;
            case LIFESTEAL -> SoundEvents.PLAYER_LEVELUP;
            case BOUNCE -> SoundEvents.PISTON_EXTEND;
            case WEAKEN -> SoundEvents.WITHER_SHOOT;
            default -> null;
        };
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), elementSound, SoundSource.PLAYERS, 0.8f, 1.0f);
        if (modSound != null) p.level().playSound(null, p.getX(), p.getY(), p.getZ(), modSound, SoundSource.PLAYERS, 0.6f, 1.4f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.5f, 0.8f);
    }

    private static SimpleParticleType getElementParticle(SkillTags.Element e) {
        return switch (e) {
            case FIRE -> ParticleTypes.FLAME; case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK; case VOID -> ParticleTypes.PORTAL;
            case FORCE -> ParticleTypes.SOUL_FIRE_FLAME; default -> ParticleTypes.CRIT;
        };
    }

    private static SimpleParticleType getModifierParticle(SkillTags.Modifier mod) {
        return switch (mod) {
            case EXPLODE -> ParticleTypes.LAVA; case STUN -> ParticleTypes.WARPED_SPORE;
            case LIFESTEAL -> ParticleTypes.HEART;
            case WEAKEN -> ParticleTypes.SMOKE;
            case BOUNCE -> ParticleTypes.POOF;
            default -> ParticleTypes.WHITE_ASH;
        };
    }

    private static int getBaseShapeCooldown(SkillTags.Shape s) {
        return switch (s) {
            case PUNCH, DASH -> 15;
            case SLASH, VERT_SLASH, HORIZ_SLASH -> 25;
            case SINGLE, BEAM, BALL, RAY -> 45;
            case CONE, IMPACT_BURST, FLARE, BOOMERANG -> 60;
            case WALL, SPIKES, BLINK_STRIKE -> 80;
            case BARRAGE, BARRAGE_PUNCH, SLASH_BARRAGE, RAIN, AOE -> 140;
            default -> 40;
        };
    }

    public static String getSkillName(String recipe) {
        if (recipe == null || recipe.isEmpty()) return "Unknown Art";
        try {
            String[] parts = recipe.split(":");
            return formatName(parts[2]) + " " + formatName(parts[1]) + " " + formatName(parts[0]);
        } catch (Exception e) { return "Unnamed Art"; }
    }

    private static String formatName(String text) {
        text = text.replace("_", " ");
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}