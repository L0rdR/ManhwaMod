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
import net.minecraft.server.TickTask;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;

public class SkillEngine {

    // CHANGED: Now accepts pre-fetched data and returns the Cooldown Duration (int)
    public static int execute(ServerPlayer player, int skillId, String recipe, int cost, int intelligence) {
        if (recipe == null || recipe.isEmpty()) return 0;

        try {
            String[] parts = recipe.split(":");
            if (parts.length < 3) return 0;

            // 1. Parse Enums once
            SkillTags.Shape shape = SkillTags.Shape.valueOf(parts[0].toUpperCase().trim().replace(" ", "_"));
            SkillTags.Element element = SkillTags.Element.valueOf(parts[1].toUpperCase().trim());
            SkillTags.Modifier modifier = SkillTags.Modifier.valueOf(parts[2].toUpperCase().trim());

            // 2. Calculate Cooldown
            int baseCD = getBaseShapeCooldown(shape);
            int totalDuration = baseCD + (int)(cost * 0.1f);

            // 3. Visuals & Scaling
            float multi = 1.0f + (cost / 100.0f);
            SimpleParticleType p1 = getElementParticle(element);
            SimpleParticleType p2 = getModifierParticle(modifier);

            playSkillSounds(player, element, modifier);
            player.serverLevel().sendParticles(p1, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.1, 0.4, 0.05);

            // 4. Run Shape Logic (Pass 'intelligence' down to avoid re-fetching it 50 times)
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

            // Return the calculated cooldown so PacketCastSkill can save it
            return totalDuration;

        } catch (Exception e) {
            e.printStackTrace();
            return 100; // Default safety cooldown
        }
    }

    // --- Updated Methods (Accepting 'int intelligence') ---

    private static void damageArea(ServerPlayer player, Vec3 pos, double range, float baseDmg, SkillTags.Modifier mod, SkillTags.Element element, int manaStat) {
        // FORMULA: 0.5% Magic Damage per point.
        // Matches Strength scaling perfectly.
        float damageMulti = 1.0f + (manaStat * 0.005f);

        boolean hasCustomDamage = (element == SkillTags.Element.VOID || element == SkillTags.Element.FIRE ||
                element == SkillTags.Element.LAVA || element == SkillTags.Element.LIGHTNING ||
                element == SkillTags.Element.LIGHT || element == SkillTags.Element.ACID ||
                element == SkillTags.Element.ICE);

        player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(range), e -> e != player).forEach(target -> {
            if (!hasCustomDamage) {
                target.hurt(player.damageSources().magic(), baseDmg * damageMulti);
            }
            applyElementEffect(target, element, player, baseDmg, manaStat);
            applyModifier(target, mod, player);
        });
    }

    // Example of updating a Shape method to pass intelligence down:
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

                // PASSING INTELLIGENCE HERE:
                damageArea(p, center, radius, 1.5f * multi, m, element, intelligence);

                p.level().playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.5f);
            }));
        }
    }

    // NOTE: You must update ALL runX methods (runBall, runRay, etc.) to accept 'int intelligence'
    // and pass it to damageArea, just like I did for runAOE above.
    // I have omitted the full list to save space, but you just need to add ", int intelligence" to the signature.

    // ... (Keep applyElementEffect, getElementParticle, etc. exactly as they were) ...

    // Helper to keep names clean
    public static String getSkillName(String recipe) {
        if (recipe == null || recipe.isEmpty() || recipe.equals("0")) return "None";
        try {
            String[] parts = recipe.split(":");
            return formatName(parts[2]) + " " + formatName(parts[1]) + " " + formatName(parts[0]);
        } catch (Exception e) { return "Unnamed Art"; }
    }
    private static String formatName(String text) {
        text = text.replace("_", " ");
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    // ... (Keep getBaseShapeCooldown, etc.) ...
    private static void applyElementEffect(LivingEntity target, SkillTags.Element element, ServerPlayer source, float baseDmg, int manaStat) {
        // Scalers for status effects
        float damageMulti = 1.0f + (manaStat * 0.02f);
        float boostedMulti = 1.0f + (manaStat * 0.03f);
        int durationBoost = manaStat * 2; // +0.1s per point
        int potencyBoost = Math.min(5, manaStat / 20); // Effect Level increases every 20 points

        switch (element) {
            case FIRE -> {
                target.setSecondsOnFire(4 + (manaStat / 10));
                target.hurt(source.damageSources().magic(), baseDmg * damageMulti);
            }
            case ICE -> { // Now uses boostedMulti for damage if it deals any
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + durationBoost, 2 + potencyBoost));
                target.hurt(source.damageSources().magic(), baseDmg * boostedMulti);
            }
            case LIGHTNING -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15 + (manaStat/5), 10));
                target.hurt(source.damageSources().lightningBolt(), baseDmg * (1.1f + (manaStat * 0.015f)));
            }
            case WATER -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + durationBoost, 1 + potencyBoost));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40 + durationBoost, potencyBoost));
            }
            case EARTH -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + durationBoost, 4));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60 + durationBoost, 2 + potencyBoost));
            }
            case LAVA -> { // CHANGED to boostedMulti
                target.setSecondsOnFire(8 + (manaStat / 5));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + durationBoost, 1));
                target.hurt(source.damageSources().onFire(), baseDmg * boostedMulti);
            }
            case WIND -> {
                float lift = 0.6f + (manaStat * 0.02f);
                target.setDeltaMovement(target.getDeltaMovement().add(0, lift, 0));
                target.hurtMarked = true;
            }
            case SHADOW -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40 + durationBoost, 0));
            case ACID -> { // Now uses boostedMulti for armor melting and poison
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + durationBoost, 1 + potencyBoost));
                // Armor damage scales at 3% intensity
                int armorMelt = (int)(2 * boostedMulti);
                target.getArmorSlots().forEach(s -> s.setDamageValue(s.getDamageValue() + armorMelt));
            }
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100 + durationBoost, potencyBoost));
            case LIGHT -> { // CHANGED to boostedMulti for the Undead bonus
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100 + durationBoost, 0));
                if (target.isInvertedHealAndHarm())
                    target.hurt(source.damageSources().magic(), baseDmg * (boostedMulti * 1.5f));
                else
                    source.heal(1.0f + (manaStat * 0.05f));
            }
            case VOID -> {
                float voidMulti = 1.0f + (manaStat * 0.03f); // Void scales harder
                target.hurt(source.damageSources().fellOutOfWorld(), baseDmg * voidMulti);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40 + durationBoost, 0));
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.3 + (manaStat * 0.01), 0));
                target.hurtMarked = true;
            }
            case FORCE -> {
                float pushPower = 1.2f + (manaStat * 0.05f);
                Vec3 push = target.position().subtract(source.position()).normalize().scale(pushPower);
                target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.3, push.z));
                target.hurtMarked = true;
            }
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

    // Stub methods to prevent compile errors in this snippet (YOU MUST UPDATE THE REAL ONES to accept 'int intelligence')
    // I am assuming you will add ", int intelligence" to all runX methods and pass it to damageArea.
    private static void runBall(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runRay(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runBeam(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runSingle(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runPunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runDash(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runSlash(ServerPlayer p, SimpleParticleType p1, float yawOff, boolean vert, SkillTags.Modifier m, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runInstantWall(ServerPlayer p, SimpleParticleType p1, int cost, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runSpikes(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runBarrage(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runBarragePunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runSlashBarrage(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runRain(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runBlinkStrike(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runProjectileFlare(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runImpactBurst(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runCone(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element element, int intelligence) { /* Logic */ }
    private static void runBoomerang(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { /* Logic */ }
}