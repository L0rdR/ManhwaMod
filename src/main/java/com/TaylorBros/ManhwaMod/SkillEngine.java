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

public class SkillEngine {

    public static void execute(ServerPlayer player, int skillId) {
        String recipe = player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId);
        if (recipe.isEmpty()) return;

        String[] parts = recipe.split(":");
        SkillTags.Shape shape = SkillTags.Shape.valueOf(parts[0]);
        SkillTags.Element element = SkillTags.Element.valueOf(parts[1]);
        SkillTags.Modifier modifier = SkillTags.Modifier.valueOf(parts[2]);

        int cost = player.getPersistentData().getInt("manhwamod.skill_cost_" + skillId);
        float powerMultiplier = 1.0f + (cost / 100.0f);

        // --- PLAY LAYERED SOUNDS ---
        playSkillSounds(player, element, modifier);

        // --- DYNAMIC PARTICLE MIXING ---
        SimpleParticleType mainParticle = switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case VOID -> ParticleTypes.PORTAL;
            case FORCE -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.CRIT;
        };

        SimpleParticleType modParticle = switch (modifier) {
            case EXPLODE -> ParticleTypes.LARGE_SMOKE;
            case STUN -> ParticleTypes.ENCHANTED_HIT;
            case LIFESTEAL -> ParticleTypes.HEART;
            case WEAKEN -> ParticleTypes.SQUID_INK;
            default -> mainParticle;
        };

        // --- SHAPE EXECUTION (Now with Cost for Range) ---
        switch (shape) {
            case BEAM -> runBeam(player, mainParticle, modParticle, modifier, powerMultiplier, cost);
            case AOE -> runAOE(player, mainParticle, modParticle, modifier, powerMultiplier, cost);
            case CONE -> runCone(player, mainParticle, modParticle, modifier, powerMultiplier, cost);
            case SINGLE -> runSingle(player, mainParticle, modParticle, modifier, powerMultiplier, cost);
        }
    }

    private static void playSkillSounds(ServerPlayer player, SkillTags.Element element, SkillTags.Modifier modifier) {
        // 1. BASE ELEMENT SOUND
        switch (element) {
            case FIRE -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            case ICE -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.5f);
            case LIGHTNING -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 2.0f);
            case VOID -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.5f);
            case FORCE -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.5f);
        }

        // 2. MODIFIER "IMPACT" SOUND
        switch (modifier) {
            case EXPLODE -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.2f);
            case LIFESTEAL -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1.0f, 1.0f);
            case STUN -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.8f);
            case WEAKEN -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.7f, 0.5f);
        }
    }

    private static void runBeam(ServerPlayer player, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier mod, float multi, int cost) {
        int range = 10 + (cost / 10); // Mana makes the beam longer
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();

        for (int i = 0; i < range; i++) {
            Vec3 point = start.add(look.scale(i));
            player.serverLevel().sendParticles(p1, point.x, point.y, point.z, 5, 0.1, 0.1, 0.1, 0.05);
            player.serverLevel().sendParticles(p2, point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.02);

            AABB area = new AABB(point, point).inflate(0.5);
            player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player).forEach(target -> {
                target.hurt(player.damageSources().magic(), 5.0f * multi);
                applyModifier(target, mod, player);
            });
        }
    }

    private static void runAOE(ServerPlayer player, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier mod, float multi, int cost) {
        float radius = 3.0f + (cost / 25.0f); // Mana makes the circle bigger
        for (int i = 0; i < 360; i += 20) {
            double rad = Math.toRadians(i);
            double px = player.getX() + Math.cos(rad) * radius;
            double pz = player.getZ() + Math.sin(rad) * radius;
            player.serverLevel().sendParticles(p1, px, player.getY() + 1, pz, 5, 0.1, 0.1, 0.1, 0.1);
            player.serverLevel().sendParticles(p2, px, player.getY() + 1, pz, 3, 0.1, 0.1, 0.1, 0.1);
        }

        AABB area = player.getBoundingBox().inflate(radius);
        player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player).forEach(target -> {
            target.hurt(player.damageSources().magic(), 4.0f * multi);
            applyModifier(target, mod, player);
        });
    }

    private static void runCone(ServerPlayer player, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier mod, float multi, int cost) {
        int length = 5 + (cost / 15); // Mana makes the blast deeper
        int spread = 30 + (cost / 5);  // Mana makes the blast wider
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();

        for (int arc = -spread; arc <= spread; arc += 10) {
            float yaw = player.getYRot() + arc;
            double xDir = -Math.sin(Math.toRadians(yaw));
            double zDir = Math.cos(Math.toRadians(yaw));
            Vec3 spreadDir = new Vec3(xDir, look.y, zDir).normalize();

            for (int dist = 1; dist < length; dist++) {
                Vec3 point = start.add(spreadDir.scale(dist));
                player.serverLevel().sendParticles(p1, point.x, point.y, point.z, 2, 0.2, 0.2, 0.2, 0.02);
                player.serverLevel().sendParticles(p2, point.x, point.y, point.z, 1, 0.2, 0.2, 0.2, 0.02);

                AABB area = new AABB(point, point).inflate(1.0);
                player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player).forEach(target -> {
                    target.hurt(player.damageSources().magic(), 3.0f * multi);
                    applyModifier(target, mod, player);
                });
            }
        }
    }

    private static void runSingle(ServerPlayer player, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier mod, float multi, int cost) {
        int reach = 20 + (cost / 5); // Mana makes the target search further
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(reach));
        AABB searchArea = player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0);

        LivingEntity target = null;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchArea, e -> e != player)) {
            if (entity.getBoundingBox().clip(start, end).isPresent()) {
                target = entity;
                break;
            }
        }

        if (target != null) {
            player.serverLevel().sendParticles(p1, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            player.serverLevel().sendParticles(p2, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            target.hurt(player.damageSources().magic(), 10.0f * multi);
            applyModifier(target, mod, player);
        }
    }

    private static void applyModifier(LivingEntity target, SkillTags.Modifier mod, ServerPlayer player) {
        switch (mod) {
            case EXPLODE -> target.level().explode(null, target.getX(), target.getY(), target.getZ(), 1.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            case STUN -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10));
            case WEAKEN -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            case LIFESTEAL -> {
                player.heal(1.5f);
                player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    public static String getSkillName(String recipe) {
        if (recipe.isEmpty()) return "Unknown Skill";
        String[] parts = recipe.split(":");
        return parts[1] + " " + parts[0];
    }

    public static String getSkillDescription(String recipe) {
        if (recipe.isEmpty()) return "";
        String[] parts = recipe.split(":");
        return "§7Type: §f" + parts[0] + "\n§7Element: §f" + parts[1] + "\n§7Effect: §6" + parts[2];
    }
}