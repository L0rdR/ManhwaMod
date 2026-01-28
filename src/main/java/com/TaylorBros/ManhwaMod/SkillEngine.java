package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.TickTask;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.network.chat.Component;

public class SkillEngine {

    public static SkillTags.Element currentSkillElement = SkillTags.Element.NONE;

    public static int execute(ServerPlayer player, int skillId, String recipe, int cost, int intelligence) {
        if (recipe == null || recipe.isEmpty()) return 0;

        String cleanRecipe = recipe.contains("|") ? recipe.split("\\|")[0] : recipe;

        try {
            String[] parts = cleanRecipe.split(":");
            if (parts.length < 3) return 0;

            SkillTags.Shape shape = SkillTags.Shape.valueOf(parts[0].toUpperCase().trim().replace(" ", "_"));
            SkillTags.Element element = SkillTags.Element.valueOf(parts[1].toUpperCase().trim());
            SkillTags.Modifier modifier = SkillTags.Modifier.valueOf(parts[2].toUpperCase().trim());

            int baseCD = getBaseShapeCooldown(shape);
            int totalDuration = baseCD + (int)(cost * 0.1f);

            float multi = 1.0f + (cost / 100.0f);
            SimpleParticleType p1 = getElementParticle(element);
            SimpleParticleType p2 = getModifierParticle(modifier);

            playSkillSounds(player, element, modifier);
            player.serverLevel().sendParticles(p1, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.1, 0.4, 0.05);

            switch (shape) {
                case BALL -> runBall(player, p1, p2, modifier, multi, element, intelligence);
                case RAY -> runRay(player, p1, p2, modifier, multi, element, intelligence);
                case BEAM -> runBeam(player, p1, multi, modifier, element, intelligence);
                case SINGLE -> runSingle(player, p1, multi, modifier, element, intelligence);
                case PUNCH -> runPunch(player, p1, p2, modifier, multi, element, intelligence);
                case DASH -> runDash(player, p1, modifier, multi, element, intelligence);
                case SLASH, VERT_SLASH, HORIZ_SLASH -> runSlash(player, p1, 0, shape == SkillTags.Shape.VERT_SLASH, modifier, multi, element, intelligence);
                case WALL -> runInstantWall(player, p1, cost, multi, element, intelligence);
                case SPIKES -> runSpikes(player, p1, p2, modifier, multi, cost, element, intelligence);
                case BARRAGE -> runBarrage(player, p1, p2, modifier, multi, cost, element, intelligence);
                case BARRAGE_PUNCH -> runBarragePunch(player, p1, p2, modifier, multi, cost, element, intelligence);
                case SLASH_BARRAGE -> runSlashBarrage(player, p1, modifier, multi, cost, element, intelligence);
                case RAIN -> runRain(player, p1, p2, modifier, multi, cost, element, intelligence);
                case BLINK_STRIKE -> runBlinkStrike(player, p1, modifier, multi, element, intelligence);
                case FLARE -> runProjectileFlare(player, p1, p2, modifier, multi, cost, element, intelligence);
                case IMPACT_BURST -> runImpactBurst(player, p1, p2, modifier, multi, cost, element, intelligence);
                case AOE -> runAOE(player, p1, p2, modifier, multi, cost, element, intelligence);
                case CONE -> runCone(player, p1, modifier, multi, element, intelligence);
                case BOOMERANG -> runBoomerang(player, p1, p2, modifier, multi, cost, element, intelligence);
                default -> runSingle(player, p1, multi, modifier, element, intelligence);
            }
            return totalDuration;

        } catch (Exception e) {
            e.printStackTrace();
            return 100;
        }
    }

    // --- DAMAGE LOGIC ---
    private static void damageArea(ServerPlayer player, Vec3 pos, double range, float baseDmg, SkillTags.Modifier mod, SkillTags.Element element, int manaStat) {
        float damageMulti = 1.0f + (manaStat * 0.005f);

        // Tell the "Referee" (Combat Event) what element we are using right now
        currentSkillElement = element;

        player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(range), e -> e != player).forEach(target -> {

            // FIXED: Using player.damageSources().magic() directly
            // This avoids the .source() and .level() calls that were likely causing the red text
            target.hurt(player.damageSources().magic(), baseDmg * damageMulti);

            applyElementEffect(target, element, player, baseDmg, manaStat);
            applyModifier(target, mod, player);
        });

        // Reset the bridge to NONE so regular sword hits don't accidentally get elemental buffs
        currentSkillElement = SkillTags.Element.NONE;
    }

    // --- SHAPE IMPLEMENTATIONS ---

    private static void runSingle(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element e, int intel) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = 1; i < 20; i++) {
            Vec3 pos = start.add(look.scale(i));
            p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            if (!p.level().noCollision(new AABB(pos, pos).inflate(0.5))) {
                damageArea(p, pos, 2.0, 6.0f * multi, m, e, intel);
                break;
            }
        }
    }

    private static void runBall(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) {
        Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(2.0));
        p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 10, 0.5, 0.5, 0.5, 0.1);
        shootProjectile(p, p1, m, 8.0f * multi, 1.5, e, intel);
    }

    private static void shootProjectile(ServerPlayer p, SimpleParticleType particle, SkillTags.Modifier mod, float dmg, double speed, SkillTags.Element ele, int intel) {
        Vec3 start = p.getEyePosition();
        Vec3 vel = p.getLookAngle().scale(speed);
        for (int i=0; i<40; i++) {
            final int tick = i;
            p.server.tell(new TickTask(p.server.getTickCount() + i, () -> {
                Vec3 current = start.add(vel.scale(tick));
                p.serverLevel().sendParticles(particle, current.x, current.y, current.z, 2, 0.1, 0.1, 0.1, 0);
                damageArea(p, current, 1.5, dmg, mod, ele, intel);
            }));
        }
    }

    private static void runRay(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < 30; i++) {
            Vec3 pos = start.add(look.scale(i));
            p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0);
            if (i % 3 == 0) damageArea(p, pos, 1.5, 3.0f * multi, m, e, intel);
        }
    }

    private static void runBeam(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element e, int intel) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < 50; i++) {
            Vec3 pos = start.add(look.scale(i));
            p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0);
            damageArea(p, pos, 2.0, 1.0f * multi, m, e, intel);
        }
    }

    private static void runPunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) {
        Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(2.0));
        p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 10, 0.5, 0.5, 0.5, 0.2);
        p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 5, 0.3, 0.3, 0.3, 0.1);
        damageArea(p, pos, 3.0, 10.0f * multi, m, e, intel);
    }

    private static void runDash(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) {
        Vec3 look = p.getLookAngle().scale(2.5);
        p.setDeltaMovement(look);
        p.hurtMarked = true;
        damageArea(p, p.position(), 3.0, 5.0f * multi, m, e, intel);
    }

    private static void runSlash(ServerPlayer p, SimpleParticleType p1, float yawOff, boolean vert, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = -3; i <= 3; i++) {
            double offset = i * 0.5;
            Vec3 pos = start.add(look.scale(3)).add(vert ? 0 : offset, vert ? offset : 0, 0);
            p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0);
            damageArea(p, pos, 2.0, 8.0f * multi, m, e, intel);
        }
    }

    private static void runInstantWall(ServerPlayer p, SimpleParticleType p1, int cost, float multi, SkillTags.Element e, int intel) {
        Vec3 pos = p.position().add(p.getLookAngle().scale(3));
        for (int y = 0; y < 3; y++) {
            for (int x = -2; x <= 2; x++) {
                p.serverLevel().sendParticles(p1, pos.x + x, pos.y + y, pos.z, 5, 0.2, 0.2, 0.2, 0);
                damageArea(p, pos.add(x, y, 0), 1.5, 4.0f * multi, SkillTags.Modifier.NONE, e, intel);
            }
        }
    }

    // FIXED: Added 'final int dist = i' to prevent compilation error
    private static void runSpikes(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        Vec3 start = p.position();
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        for (int i = 1; i < 10; i++) {
            final int dist = i; // Fix for lambda variable
            p.server.tell(new TickTask(p.server.getTickCount() + i, () -> {
                Vec3 pos = start.add(dir.scale(dist * 1.5));
                p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 10, 0.2, 0.5, 0.2, 0.1);
                damageArea(p, pos, 2.0, 6.0f * multi, m, e, intel);
            }));
        }
    }

    private static void runBarrage(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        for (int i = 0; i < 10; i++) {
            final int delay = i * 3;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                shootProjectile(p, p1, m, 3.0f * multi, 2.0, e, intel);
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5f, 1.5f);
            }));
        }
    }

    private static void runBarragePunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        for (int i = 0; i < 8; i++) {
            final int delay = i * 2;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                Vec3 offset = new Vec3((Math.random()-0.5)*2, (Math.random()-0.5)*2, (Math.random()-0.5)*2);
                Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(2)).add(offset);
                p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0);
                damageArea(p, pos, 2.0, 5.0f * multi, m, e, intel);
            }));
        }
    }

    private static void runSlashBarrage(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        for (int i = 0; i < 5; i++) {
            final int index = i;
            final int delay = i * 4;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                runSlash(p, p1, 0, index % 2 == 0, m, multi, e, intel);
            }));
        }
    }

    private static void runRain(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        Vec3 center = p.position();
        for (int i = 0; i < 20; i++) {
            final int delay = i * 2;
            p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> {
                double x = center.x + (Math.random() - 0.5) * 10;
                double z = center.z + (Math.random() - 0.5) * 10;
                p.serverLevel().sendParticles(p1, x, center.y + 5, z, 5, 0.1, 1.0, 0.1, 0.2);
                damageArea(p, new Vec3(x, center.y, z), 3.0, 4.0f * multi, m, e, intel);
            }));
        }
    }

    private static void runBlinkStrike(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) {
        Vec3 dest = p.getEyePosition().add(p.getLookAngle().scale(8));
        p.teleportTo(dest.x, dest.y, dest.z);
        damageArea(p, dest, 4.0, 10.0f * multi, m, e, intel);
        p.serverLevel().sendParticles(p1, dest.x, dest.y, dest.z, 20, 1.0, 1.0, 1.0, 0.1);
    }

    private static void runProjectileFlare(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        shootProjectile(p, p1, m, 8.0f * multi, 1.0, e, intel);
        p.server.tell(new TickTask(p.server.getTickCount() + 10, () -> {
            Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(10));
            p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 30, 2.0, 2.0, 2.0, 0.1);
            damageArea(p, pos, 5.0, 12.0f * multi, m, e, intel);
        }));
    }

    private static void runImpactBurst(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        damageArea(p, p.position(), 6.0, 15.0f * multi, m, e, intel);
        p.serverLevel().sendParticles(p1, p.getX(), p.getY(), p.getZ(), 50, 3.0, 0.5, 3.0, 0.2);
    }

    private static void runCone(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) {
        Vec3 start = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        for (int i = 1; i < 8; i++) {
            Vec3 center = start.add(dir.scale(i));
            double width = i * 0.5;
            p.serverLevel().sendParticles(p1, center.x, center.y, center.z, i * 2, width, 0.2, width, 0.05);
            damageArea(p, center, width, 5.0f * multi, m, e, intel);
        }
    }

    private static void runBoomerang(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) {
        shootProjectile(p, p1, m, 6.0f * multi, 1.5, e, intel);
    }

    private static void runAOE(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) {
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
                damageArea(p, center, radius, 1.5f * multi, m, element, intelligence);
                p.level().playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.5f);
            }));
        }
    }

    // --- FULLY RESTORED SOUND & EFFECT LOGIC ---

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

    private static void applyElementEffect(LivingEntity target, SkillTags.Element element, ServerPlayer source, float baseDmg, int manaStat) {
        float boostedMulti = 1.0f + (manaStat * 0.03f);
        int durationBoost = manaStat * 2;
        int potencyBoost = Math.min(5, manaStat / 20);

        switch (element) {
            case FIRE -> target.setSecondsOnFire(4 + (manaStat / 10));
            case ICE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + durationBoost, 2 + potencyBoost));
            case LIGHTNING -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15 + (manaStat/5), 10));
            case WATER -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + durationBoost, 1 + potencyBoost));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40 + durationBoost, potencyBoost));
            }
            case EARTH -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + durationBoost, 4));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60 + durationBoost, 2 + potencyBoost));
            }
            case LAVA -> {
                target.setSecondsOnFire(8 + (manaStat / 5));
                target.hurt(source.damageSources().onFire(), baseDmg * boostedMulti);
            }
            case WIND -> {
                float lift = 0.6f + (manaStat * 0.02f);
                target.setDeltaMovement(target.getDeltaMovement().add(0, lift, 0));
                target.hurtMarked = true;
            }
            case SHADOW -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40 + durationBoost, 0));
            case ACID -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + durationBoost, 1 + potencyBoost));
                target.getArmorSlots().forEach(s -> s.setDamageValue(s.getDamageValue() + 2));
            }
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100 + durationBoost, potencyBoost));
            case LIGHT -> {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100 + durationBoost, 0));
                if (target.isInvertedHealAndHarm()) target.hurt(source.damageSources().magic(), baseDmg * 1.5f);
            }
            case VOID -> {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40 + durationBoost, 0));
                target.hurt(source.damageSources().fellOutOfWorld(), baseDmg * boostedMulti);
            }
            case FORCE -> {
                Vec3 push = target.position().subtract(source.position()).normalize().scale(1.2 + (manaStat * 0.05));
                target.setDeltaMovement(target.getDeltaMovement().add(push));
                target.hurtMarked = true;
            }
        }
    }

    private static void applyModifier(LivingEntity t, SkillTags.Modifier m, ServerPlayer p) {
        switch (m) {
            case EXPLODE -> t.level().explode(null, t.getX(), t.getY(), t.getZ(), 2.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            case STUN -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 10));
            }
            case LIFESTEAL -> p.heal(1.0f);
            case WEAKEN -> {
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            case BOUNCE -> {
                Vec3 knockback = t.position().subtract(p.position()).normalize().add(0, 0.5, 0).scale(1.5);
                t.setDeltaMovement(knockback);
                t.hurtMarked = true;
            }
            case CHAIN -> {
                p.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(5.0), e -> e != p && e != t).stream().limit(3).forEach(next -> {
                    next.hurt(p.damageSources().magic(), 4.0f);
                    p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, next.getX(), next.getY() + 1, next.getZ(), 5, 0.1, 0.1, 0.1, 0.01);
                });
            }
            case VAMPIRE -> {
                p.heal(2.0f);
                p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
            }
            case GRAVITY -> {
                Vec3 center = t.position();
                p.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(7.0), e -> e != p).forEach(victim -> {
                    Vec3 pull = center.subtract(victim.position()).normalize().scale(0.8);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(pull.x, 0.2, pull.z));
                    victim.hurtMarked = true;
                });
            }
            case WITHER -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            case EXECUTE -> {
                if (t.getHealth() < (t.getMaxHealth() * 0.25f)) {
                    t.hurt(p.damageSources().magic(), 40.0f);
                    p.serverLevel().sendParticles(ParticleTypes.SONIC_BOOM, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
                }
            }
            case NONE -> {}
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

    public static String getSkillName(String recipe) {
        if (recipe == null || recipe.isEmpty() || recipe.equals("0")) return "None";

        // If the recipe already has a cool name attached, use it!
        if (recipe.contains("|")) {
            return recipe.split("\\|")[1];
        }

        try {
            // Fallback for old skills
            String[] parts = recipe.split(":");
            return formatName(parts[2]) + " " + formatName(parts[1]) + " " + formatName(parts[0]);
        } catch (Exception e) { return "Unnamed Art"; }
    }
    private static String formatName(String text) {
        text = text.replace("_", " ");
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
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

    public static SkillTags.Element rollWeightedElement(ServerPlayer player) {
        Affinity playerAff = SystemData.getAffinity(player);

        // 1. Roll for the 65% "Soul Resonance"
        if (player.getRandom().nextFloat() < 0.65f && playerAff != Affinity.NONE) {
            try {
                // Convert the Affinity name (e.g., "FIRE") to the Skill Element
                return SkillTags.Element.valueOf(playerAff.name());
            } catch (Exception e) {
                // Fallback if names don't match perfectly
                return SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)];
            }
        }

        // 2. The 35% "Chaos" roll (Pure Random)
        return SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)];
    }
}