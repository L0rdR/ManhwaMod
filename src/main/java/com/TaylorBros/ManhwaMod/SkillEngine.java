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

            // 1. First, define the cooldown key
            String cdKey = "manhwamod.cd_timer_" + skillId;
            long currentTime = player.level().getGameTime();

            // 2. Then, check if we are still on cooldown
            if (currentTime < player.getPersistentData().getLong(cdKey)) {
                player.displayClientMessage(Component.literal("§cArt Cooldown!"), true);
                return;
            }

            // 3. If we passed the check, set the NEW cooldown
            int baseCD = getBaseShapeCooldown(shape);
            player.getPersistentData().putLong(cdKey, currentTime + baseCD + (int)(cost * 0.1f));

            float multi = 1.0f + (cost / 100.0f);
            SimpleParticleType p1 = getElementParticle(element);
            SimpleParticleType p2 = getModifierParticle(modifier);
            playSkillSounds(player, element, modifier);

            player.serverLevel().sendParticles(p1, player.getX(), player.getY(), player.getZ(), 15, 0.4, 0.1, 0.4, 0.05);

            switch (shape) {
                case BALL -> runBall(player, p1, p2, modifier, multi, element);
                case RAY -> runRay(player, p1, p2, modifier, multi, element);
                case BEAM -> runBeam(player, p1, multi, modifier, element);
                case SINGLE -> runSingle(player, p1, multi, modifier, element);
                case PUNCH -> runPunch(player, p1, p2, modifier, multi, element);
                case DASH -> runDash(player, p1, modifier, multi, element);
                case SLASH, VERT_SLASH, HORIZ_SLASH -> runSlash(player, p1, 0, shape == SkillTags.Shape.VERT_SLASH, modifier, multi, element);
                case WALL -> runInstantWall(player, p1, cost, multi, element);
                case SPIKES -> runSpikes(player, p1, p2, modifier, multi, cost, element);
                case BARRAGE -> runBarrage(player, p1, p2, modifier, multi, cost, element);
                case BARRAGE_PUNCH -> runBarragePunch(player, p1, p2, modifier, multi, cost, element);
                case SLASH_BARRAGE -> runSlashBarrage(player, p1, modifier, multi, cost, element);
                case RAIN -> runRain(player, p1, p2, modifier, multi, cost, element);
                case BLINK_STRIKE -> runBlinkStrike(player, p1, modifier, multi, element);
                case FLARE -> runProjectileFlare(player, p1, p2, modifier, multi, cost, element);
                case IMPACT_BURST -> runImpactBurst(player, p1, p2, modifier, multi, cost, element);
                case AOE -> runAOE(player, p1, p2, modifier, multi, cost, element);
                case CONE -> runCone(player, p1, modifier, multi, element);
                case BOOMERANG -> runBoomerang(player, p1, p2, modifier, multi, cost, element);
                default -> runSingle(player, p1, multi, modifier, element);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void runProjectileFlare(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
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
                    damageArea(p, pos, 4.5 + (cost/40f), 14.0f * multi, m, element);
                    p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }));
        }
    }

    private static void runAOE(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        double radius = 4.0 + (cost / 40.0);
        Vec3 center = p.position();
        for (int t = 0; t < 80; t += 5) {
            final int delay = t;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;
                for (int degree = 0; degree < 360; degree += 20) {
                    double rad = Math.toRadians(degree);
                    p.serverLevel().sendParticles(p1, center.x + Math.cos(rad) * radius, center.y + 0.2, center.z + Math.sin(rad) * radius, 1, 0, 0, 0, 0);
                }
                p.serverLevel().sendParticles(p2, center.x, center.y + 0.5, center.z, 5, radius/2, 0.5, radius/2, 0.02);
                damageArea(p, center, radius, 1.5f * multi, m, element);
                p.level().playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.5f);
            }));
        }
    }

    private static void runBoomerang(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        int duration = 30;
        Vec3 startPos = p.getEyePosition();
        Vec3 forward = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
        for (int i = 0; i <= duration; i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + step, () -> {
                if (p.isRemoved()) return;
                double progress = (double) step / duration;
                double distForward = Math.sin(progress * Math.PI) * 10.0;
                double distSide = Math.sin(progress * Math.PI) * 5.0;
                Vec3 currentPos = startPos.add(forward.scale(distForward)).add(side.scale(distSide));
                for (int deg = 0; deg < 360; deg += 90) {
                    double rad = Math.toRadians(deg + (step * 45));
                    Vec3 spin = currentPos.add(new Vec3(Math.cos(rad) * 0.6, 0, Math.sin(rad) * 0.6));
                    p.serverLevel().sendParticles(p1, spin.x, spin.y, spin.z, 1, 0, 0, 0, 0);
                }
                p.serverLevel().sendParticles(p2, currentPos.x, currentPos.y, currentPos.z, 2, 0.1, 0.1, 0.1, 0.02);
                damageArea(p, currentPos, 1.8, 6.0f * multi, m, element);
                if (step % 3 == 0) p.level().playSound(null, currentPos.x, currentPos.y, currentPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.2f + (float)progress);
            }));
        }
    }

    private static void runBeam(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element element) {
        for (int t = 0; t < 20; t++) {
            final int tick = t;
            p.server.tell(new TickTask(p.server.getTickCount() + tick, () -> {
                if (p.isRemoved()) return;
                Vec3 start = p.getEyePosition();
                Vec3 look = p.getLookAngle();
                for (double d = 0; d < 15; d += 1.0) {
                    Vec3 pt = start.add(look.scale(d));
                    p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 2, 0.1, 0.1, 0.1, 0.02);
                    damageArea(p, pt, 1.2, 1.5f * multi, m, element);
                }
            }));
        }
    }

    private static void runBall(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element element) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        int lifetime = 30;
        for (int i = 0; i < lifetime; i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + step, () -> {
                if (p.isRemoved()) return;
                Vec3 pos = start.add(look.scale(step * 0.5));
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 15, 0.2, 0.2, 0.2, 0.05);
                p.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(5.0), e -> e != p).forEach(t -> {
                    t.setDeltaMovement(t.getDeltaMovement().add(pos.subtract(t.position()).normalize().scale(0.2)));
                    t.hurtMarked = true;
                    t.hurt(p.damageSources().magic(), 1.0f * multi);
                });
                if (step == lifetime - 1) {
                    p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 30, 1.5, 1.5, 1.5, 0.1);
                    damageArea(p, pos, 4.0, 10.0f * multi, m, element);
                    p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.5f);
                }
            }));
        }
    }

    private static void runBarrage(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        int totalVolleys = 4 + (cost / 30);
        Vec3 look = p.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 up = look.cross(right).normalize();
        Vec3[] arcPoints = new Vec3[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(-60 + (i * 24));
            arcPoints[i] = p.getEyePosition().add(right.scale(Math.cos(angle) * 1.5)).add(up.scale(Math.sin(angle) * 1.5));
        }
        for (int volley = 0; volley < totalVolleys; volley++) {
            final int v = volley;
            p.server.tell(new TickTask(p.server.getTickCount() + (v * 5), () -> {
                if (p.isRemoved()) return;
                for (Vec3 startPos : arcPoints) {
                    for (double d = 0; d < 6; d += 0.5) {
                        Vec3 beamPt = startPos.add(look.scale(d));
                        p.serverLevel().sendParticles(p1, beamPt.x, beamPt.y, beamPt.z, 1, 0, 0, 0, 0.01);
                        damageArea(p, beamPt, 0.8, 2.0f * multi, m, element);
                    }
                }
            }));
        }
    }

    private static void runInstantWall(ServerPlayer player, SimpleParticleType p1, int cost, float multi, SkillTags.Element element) {
        Vec3 center = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
        int width = 2 + (cost / 50);
        player.serverLevel().sendParticles(p1, center.x, center.y + 1, center.z, 60, width, 1.5, width, 0.05);
        damageArea(player, center, width, 4.0f * multi, SkillTags.Modifier.NONE, element);
    }

    private static void runSpikes(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        Vec3 direction = p.getLookAngle().multiply(1, 0, 1).normalize();
        for (int i = 1; i <= (6 + cost/15); i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + (step * 2), () -> {
                if (p.isRemoved()) return;
                Vec3 pos = p.position().add(direction.scale(step * 1.2));
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 20, 0.2, 1.5, 0.2, 0.1);
                damageArea(p, pos, 1.5, 6.0f * multi, m, element);
                p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, 0.6f, 0.8f);
            }));
        }
    }

    private static void runRain(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        BlockHitResult hit = p.level().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(25)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 center = hit.getLocation();
        for (int i = 0; i < (8 + cost/15); i++) {
            final int delay = 10 + (i * 3);
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                Vec3 strikePos = center.add((p.getRandom().nextDouble()-0.5)*8, 0, (p.getRandom().nextDouble()-0.5)*8);
                for (double h = 0; h < 12; h++) p.serverLevel().sendParticles(p1, strikePos.x, strikePos.y+h, strikePos.z, 3, 0.05, 0.5, 0.05, 0.02);
                damageArea(p, strikePos, 2.5, 5.0f * multi, m, element);
            }));
        }
    }

    private static void runCone(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element element) {
        for (int t = 0; t < 15; t++) {
            final int delay = t;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                if (p.isRemoved()) return;
                for (int angle = -25; angle <= 25; angle += 10) {
                    Vec3 dir = Vec3.directionFromRotation(p.getXRot(), p.getYRot() + angle);
                    Vec3 pt = p.getEyePosition().add(dir.scale(4));
                    p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 2, 0.4, 0.4, 0.4, 0.02);
                    damageArea(p, pt, 1.5, 1.2f * multi, m, element);
                }
            }));
        }
    }

    private static void runBlinkStrike(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier mod, float multi, SkillTags.Element element) {
        Vec3 dest = p.getEyePosition().add(p.getLookAngle().scale(10));
        BlockHitResult hit = p.level().clip(new ClipContext(p.getEyePosition(), dest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 finalPos = hit.getLocation().subtract(p.getLookAngle().scale(1.0));
        p.teleportTo(finalPos.x, finalPos.y, finalPos.z);
        damageArea(p, finalPos, 3.5, 12.0f * multi, mod, element);
        p.serverLevel().sendParticles(p1, finalPos.x, finalPos.y + 1, finalPos.z, 20, 0.5, 0.5, 0.5, 0.1);
    }

    private static void runPunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element element) {
        Vec3 pt = p.getEyePosition().add(p.getLookAngle().scale(1.5));
        damageArea(p, pt, 2.0, 9.0f * multi, m, element);
        p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 10, 0.3, 0.3, 0.3, 0.05);
    }

    private static void runDash(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element element) {
        p.setDeltaMovement(p.getLookAngle().normalize().scale(2.2));
        p.hurtMarked = true;
        for (int i = 0; i < 6; i++) {
            p.server.tell(new TickTask(p.server.getTickCount() + i, () -> {
                p.serverLevel().sendParticles(p1, p.getX(), p.getY() + 1, p.getZ(), 10, 0.2, 0.5, 0.2, 0.02);
                damageArea(p, p.position(), 2.5, 4.0f * multi, m, element);
            }));
        }
    }

    private static void runSlash(ServerPlayer p, SimpleParticleType p1, float yawOff, boolean vert, SkillTags.Modifier m, float multi, SkillTags.Element element) {
        for (int i = -35; i <= 35; i += 15) {
            float currentYaw = vert ? p.getYRot() + yawOff : p.getYRot() + i + yawOff;
            float currentPitch = vert ? p.getXRot() + i : p.getXRot();
            Vec3 dir = Vec3.directionFromRotation(currentPitch, currentYaw);
            Vec3 pt = p.getEyePosition().add(dir.scale(3));
            p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 5, 0.5, 0.5, 0.5, 0.02);
            damageArea(p, pt, 1.5, 7.0f * multi, m, element);
        }
    }

    private static void runSlashBarrage(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        for (int i = 0; i < (5 + cost/25); i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + (i * 4), () -> {
                runSlash(p, p1, 0, step % 2 != 0, m, multi * 0.7f, element);
            }));
        }
    }

    private static void runBarragePunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        for (int i = 0; i < (12 + cost/10); i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + (i * 2), () -> {
                Vec3 punchPos = p.getEyePosition().add(p.getLookAngle().scale(2.5));
                damageArea(p, punchPos, 1.5, 3.0f * multi, m, element);
                p.serverLevel().sendParticles(p1, punchPos.x, punchPos.y, punchPos.z, 5, 0.1, 0.1, 0.1, 0.02);
            }));
        }
    }

    private static void runRay(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element element) {
        Vec3 start = p.getEyePosition().add(p.getLookAngle().scale(0.5));
        Vec3 look = p.getLookAngle();
        double maxDist = 40.0;
        Vec3 end = start.add(look.scale(maxDist));

        // 1. Check for Blocks first to get the limit
        BlockHitResult blockHit = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 targetPos = blockHit.getLocation();
        double limitDist = blockHit.getLocation().distanceTo(start);

        // 2. Check for Entities along that same line, but don't go past the block
        LivingEntity targetEntity = null;
        AABB searchBox = new AABB(start, targetPos).inflate(1.0); // Search the corridor of the ray

        for (LivingEntity potentialTarget : p.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != p && e.isAlive())) {
            AABB hitBox = potentialTarget.getBoundingBox().inflate(0.3); // Slight grace for hit detection
            java.util.Optional<Vec3> clip = hitBox.clip(start, targetPos);

            if (clip.isPresent()) {
                double distToMob = clip.get().distanceTo(start);
                if (distToMob < limitDist) {
                    limitDist = distToMob; // The Ray stops here now
                    targetPos = clip.get();
                    targetEntity = potentialTarget;
                }
            }
        }

        // 3. Visuals: Draw the beam only up to the point of impact
        for (double d = 0; d < limitDist; d += 0.5) {
            Vec3 pt = start.add(look.scale(d));
            p.serverLevel().sendParticles(p1, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }

        // 4. THE EXPLOSION (Now happens at targetPos, which is the Mob's chest or the floor)
        p.serverLevel().sendParticles(ParticleTypes.FLASH, targetPos.x, targetPos.y, targetPos.z, 1, 0, 0, 0, 0);
        p.serverLevel().sendParticles(p2, targetPos.x, targetPos.y, targetPos.z, 20, 0.3, 0.3, 0.3, 0.05);

        // Impact Sound
        p.level().playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.5f);

        // 5. Logic: Damage the impact zone
        damageArea(p, targetPos, 2.5, 12.0f * multi, m, element);
    }

    private static void runSingle(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element element) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < 10; i++) {
            final int step = i;
            p.server.tell(new TickTask(p.server.getTickCount() + step, () -> {
                Vec3 pos = start.add(look.scale(step * 2.0));
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 5, 0.05, 0.05, 0.05, 0.02);
                damageArea(p, pos, 1.0, 6.0f * multi, m, element);
            }));
        }
    }

    private static void runImpactBurst(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element) {
        double maxRadius = 4.0 + (cost / 30.0);
        for (int r = 1; r <= 5; r++) {
            final double rad = (maxRadius / 5.0) * r;
            p.server.tell(new TickTask(p.server.getTickCount() + r, () -> {
                damageArea(p, p.position(), rad, 8.0f * multi, m, element);
                for (int d = 0; d < 360; d += 20) {
                    p.serverLevel().sendParticles(p1, p.getX() + Math.cos(Math.toRadians(d)) * rad, p.getY() + 0.5, p.getZ() + Math.sin(Math.toRadians(d)) * rad, 1, 0, 0, 0, 0);
                }
            }));
        }
    }

    private static void damageArea(ServerPlayer player, Vec3 pos, double range, float dmg, SkillTags.Modifier mod, SkillTags.Element element) {
        player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(range), e -> e != player).forEach(t -> {
            t.hurt(player.damageSources().magic(), dmg);
            applyElementEffect(t, element, player, dmg);
            applyModifier(t, mod, player);
        });
    }

    private static void applyElementEffect(LivingEntity target, SkillTags.Element element, ServerPlayer source, float baseDmg) {
        switch (element) {
            case FIRE -> target.setSecondsOnFire(4);
            case ICE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            case LIGHTNING -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10));
            case WATER -> { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0)); }
            case EARTH -> { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)); target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 2)); }
            case LAVA -> { target.setSecondsOnFire(8); target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)); }
            case WIND -> { target.setDeltaMovement(target.getDeltaMovement().add(0, 0.6, 0)); target.hurtMarked = true; }
            case SHADOW -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            case ACID -> { target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1)); target.getArmorSlots().forEach(s -> s.setDamageValue(s.getDamageValue() + 2)); }
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            case LIGHT -> { target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0)); if (target.isInvertedHealAndHarm()) target.hurt(source.damageSources().magic(), baseDmg * 0.5f); }
            case VOID -> { Vec3 pull = source.position().subtract(target.position()).normalize().scale(0.5); target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.2, pull.z)); target.hurtMarked = true; }
            case FORCE -> { Vec3 push = target.position().subtract(source.position()).normalize().scale(1.2); target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.3, push.z)); target.hurtMarked = true; }
        }
    }

    private static SimpleParticleType getElementParticle(SkillTags.Element e) {
        return switch (e) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case VOID -> ParticleTypes.PORTAL;
            case FORCE -> ParticleTypes.SOUL_FIRE_FLAME;
            case WATER -> ParticleTypes.SPLASH;
            case EARTH -> ParticleTypes.MYCELIUM;
            case LAVA -> ParticleTypes.LAVA;
            case LIGHT -> ParticleTypes.END_ROD;
            case WIND -> ParticleTypes.CLOUD;
            case SHADOW -> ParticleTypes.SQUID_INK;
            case ACID -> ParticleTypes.SNEEZE;
            case POISON -> ParticleTypes.ENTITY_EFFECT;
            default -> ParticleTypes.SMOKE;
        };
    }

    private static void applyModifier(LivingEntity t, SkillTags.Modifier m, ServerPlayer p) {
        switch (m) {
            case EXPLODE -> {
                t.level().explode(null, t.getX(), t.getY(), t.getZ(), 2.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
            case STUN -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, false));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 10, false, false));
            }
            case LIFESTEAL -> {
                p.heal(1.0f); // Heals half a heart
            }
            case WEAKEN -> {
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            case BOUNCE -> {
                // Flings the enemy away and slightly upward
                Vec3 knockback = t.position().subtract(p.position()).normalize().add(0, 0.5, 0).scale(1.5);
                t.setDeltaMovement(knockback);
                t.hurtMarked = true;
            }
            case CHAIN -> {
                // Arcs damage to 3 nearby enemies
                p.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(5.0), e -> e != p && e != t).stream().limit(3).forEach(next -> {
                    next.hurt(p.damageSources().magic(), 4.0f);
                    p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, next.getX(), next.getY() + 1, next.getZ(), 5, 0.1, 0.1, 0.1, 0.01);
                });
            }
            case VAMPIRE -> {
                p.heal(2.0f); // Heals a full heart
                p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
            }
            case GRAVITY -> {
                // Pulls all nearby mobs toward the target that was hit
                Vec3 center = t.position();
                p.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(7.0), e -> e != p).forEach(victim -> {
                    Vec3 pull = center.subtract(victim.position()).normalize().scale(0.8);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(pull.x, 0.2, pull.z));
                    victim.hurtMarked = true;
                });
            }
            case WITHER -> {
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
            case EXECUTE -> {
                // Triple damage if enemy is below 25% health
                if (t.getHealth() < (t.getMaxHealth() * 0.25f)) {
                    t.hurt(p.damageSources().magic(), 40.0f);
                    p.serverLevel().sendParticles(ParticleTypes.SONIC_BOOM, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
                }
            }
            case NONE -> {
                // Do nothing
            }
        }
    }

    private static void playSkillSounds(ServerPlayer p, SkillTags.Element e, SkillTags.Modifier m) {
        net.minecraft.sounds.SoundEvent elementSound = switch(e) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case LAVA -> SoundEvents.GENERIC_BURN;
            case ICE -> SoundEvents.PLAYER_HURT_FREEZE;
            case WATER -> SoundEvents.PLAYER_SPLASH;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case EARTH -> SoundEvents.ROOTED_DIRT_BREAK;
            case WIND -> SoundEvents.ELYTRA_FLYING;
            case LIGHT -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case SHADOW -> SoundEvents.WITHER_SHOOT;
            case VOID -> SoundEvents.ENDERMAN_TELEPORT;
            case FORCE -> SoundEvents.WARDEN_ATTACK_IMPACT;
            case ACID -> SoundEvents.GENERIC_DRINK;
            case POISON -> SoundEvents.SPIDER_STEP;
            default -> SoundEvents.PLAYER_ATTACK_SWEEP;
        };
        net.minecraft.sounds.SoundEvent modSound = switch(m) {
            case EXPLODE -> SoundEvents.GENERIC_EXPLODE;
            case STUN -> SoundEvents.CONDUIT_ATTACK_TARGET;
            case LIFESTEAL -> SoundEvents.PLAYER_LEVELUP;
            case VAMPIRE -> SoundEvents.PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED;
            case CHAIN -> SoundEvents.TRIDENT_THUNDER;
            case GRAVITY -> SoundEvents.WARDEN_SONIC_CHARGE;
            case WITHER -> SoundEvents.WITHER_SKELETON_DEATH;
            case EXECUTE -> SoundEvents.IRON_GOLEM_REPAIR;
            case BOUNCE -> SoundEvents.PISTON_EXTEND;
            case WEAKEN -> SoundEvents.WITHER_SHOOT;
            default -> null;
        };
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), elementSound, SoundSource.PLAYERS, 0.8f, 1.0f);
        if (modSound != null) p.level().playSound(null, p.getX(), p.getY(), p.getZ(), modSound, SoundSource.PLAYERS, 0.6f, 1.4f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.5f, 0.8f);
    }

    private static SimpleParticleType getModifierParticle(SkillTags.Modifier mod) {
        return switch (mod) {
            case EXPLODE -> ParticleTypes.LAVA;
            case STUN -> ParticleTypes.WARPED_SPORE;
            case LIFESTEAL -> ParticleTypes.HEART;
            case WEAKEN -> ParticleTypes.SMOKE;
            case BOUNCE -> ParticleTypes.POOF;
            case VAMPIRE -> ParticleTypes.DAMAGE_INDICATOR;
            case CHAIN -> ParticleTypes.ELECTRIC_SPARK;
            case GRAVITY -> ParticleTypes.REVERSE_PORTAL;
            case WITHER -> ParticleTypes.LARGE_SMOKE;
            case EXECUTE -> ParticleTypes.SOUL;
            default -> ParticleTypes.WHITE_ASH;
        };
    }

    private static int getBaseShapeCooldown(SkillTags.Shape s) {
        return switch (s) {
            case PUNCH, DASH, SLASH -> 15;
            case VERT_SLASH, HORIZ_SLASH, SINGLE, CONE -> 25;
            case BEAM, BARRAGE, RAY -> 45;
            case IMPACT_BURST, FLARE, BOOMERANG, BALL -> 60;
            case WALL, SPIKES, BLINK_STRIKE -> 80;
            case BARRAGE_PUNCH, SLASH_BARRAGE, RAIN, AOE -> 140;
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