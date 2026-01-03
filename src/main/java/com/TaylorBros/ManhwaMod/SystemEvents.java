package com.TaylorBros.ManhwaMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ManhwaMod.MODID)
public class SystemEvents {

    private static final UUID HEALTH_UUID = UUID.fromString("44584282-057d-4c3e-8c6e-8260a925684d");
    private static final UUID SPEED_UUID = UUID.fromString("77584282-057d-4c3e-8c6e-8260a925684e");

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.getPersistentData().contains("manhwamod.points")) {
                SystemData.savePoints(player, 5);
                SystemData.saveStrength(player, 10);
                SystemData.saveHealthStat(player, 10);
                SystemData.saveMana(player, 10);
                SystemData.saveSpeed(player, 10);
                SystemData.saveDefense(player, 10);
                player.getPersistentData().putBoolean("manhwamod.awakened", false);
                player.getPersistentData().putBoolean("manhwamod.is_system_player", false);
                player.getPersistentData().putBoolean("manhwamod.failed_system_trial", false);
            }
            DailyQuestData.checkAndReset(player);
            updatePlayerStats(player);
            SystemData.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            int level = player.getPersistentData().getInt("manhwamod.level");
            if (level < 20) {
                player.getPersistentData().putBoolean("manhwamod.failed_system_trial", true);
                player.sendSystemMessage(Component.literal("§c§l[SYSTEM] §fDeath occurred before Level 20. Trial Failed."));
            }
        }
    }

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide) {
            if (player.getPersistentData().getBoolean("manhwamod.is_system_player")) {
                DailyQuestData.addKill(player);
            }

            int level = Math.max(1, player.getPersistentData().getInt("manhwamod.level"));
            int xp = player.getPersistentData().getInt("manhwamod.xp");
            int xpNeeded = 50 + (level * 10);
            xp += 10;

            if (xp >= xpNeeded && level < 1000) {
                level++;
                xp = 0;
                SystemData.savePoints(player, SystemData.getPoints(player) + 5);
                player.sendSystemMessage(Component.literal("§6§l[LEVEL UP] §fYou are now §bLevel " + level));
                checkSystemAscension(player, level);
            }
            player.getPersistentData().putInt("manhwamod.xp", xp);
            player.getPersistentData().putInt("manhwamod.level", level);
            SystemData.sync(player);
        }
    }

    private static void checkSystemAscension(Player player, int level) {
        boolean hasFailed = player.getPersistentData().getBoolean("manhwamod.failed_system_trial");
        boolean isAlreadyPlayer = player.getPersistentData().getBoolean("manhwamod.is_system_player");
        if (level >= 20 && !hasFailed && !isAlreadyPlayer) {
            player.getPersistentData().putBoolean("manhwamod.is_system_player", true);
            player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fWelcome, 'Player'."));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            DailyQuestData.checkAndReset(player);
        }
    }

    public static void updatePlayerStats(Player player) {
        if (player.level().isClientSide) return;
        var hpAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.removeModifier(HEALTH_UUID);
            double bonus = SystemData.getHealthStat(player) * 2.0;
            hpAttr.addTransientModifier(new AttributeModifier(HEALTH_UUID, "Manhwa HP", bonus, AttributeModifier.Operation.ADDITION));
        }
        var spdAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spdAttr != null) {
            spdAttr.removeModifier(SPEED_UUID);
            double bonus = SystemData.getSpeed(player) * 0.01;
            spdAttr.addTransientModifier(new AttributeModifier(SPEED_UUID, "Manhwa Speed", bonus, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    @SubscribeEvent
    public static void onCombat(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            int def = SystemData.getDefense(player);
            if (def >= 5) {
                float reductionPercentage = Math.min(30, def) / 100.0f;
                event.setAmount(event.getAmount() * (1.0f - reductionPercentage));
            }
        }
        if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide) {
            int str = SystemData.getStrength(player);
            float extraDamage = (str - 10) / 2.0f;
            if (extraDamage > 0) event.setAmount(event.getAmount() + extraDamage);
        }
    }

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;

        BlockPos mobPos = entity.blockPosition();
        double distance = Math.sqrt(mobPos.distSqr(new BlockPos(0, mobPos.getY(), 0)));

        int calculatedLevel = 1 + (int) (distance / 100);
        entity.getPersistentData().putInt("manhwamod.level", calculatedLevel);

        int scaledHP = 20 + (calculatedLevel * 4);
        var hpAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(scaledHP);
            entity.setHealth(scaledHP);
        }

        String color = "§7";
        if (calculatedLevel >= 50) color = "§a";
        if (calculatedLevel >= 150) color = "§b";
        if (calculatedLevel >= 300) color = "§d";
        if (calculatedLevel >= 500) color = "§6";

        entity.setCustomName(Component.literal(color + "[LVL " + calculatedLevel + "] §f" + entity.getType().getDescription().getString()));
        entity.setCustomNameVisible(true);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        // Fixed: simplified data merging
        newPlayer.getPersistentData().merge(oldPlayer.getPersistentData());
        SystemData.sync(newPlayer);
        updatePlayerStats(newPlayer);
    }

    // --- SKILL LOGIC ---

    public static void executeDash(Player player) {
        if (!player.onGround() || !player.getPersistentData().getBoolean("manhwamod.awakened")) return;
        int speedStat = SystemData.getSpeed(player);
        int currentMana = player.getPersistentData().getInt("manhwamod.current_mana");
        double distanceGoal = Math.min(15.0, 5.0 + (speedStat * 0.334));
        int dashCost = (int) (distanceGoal * 2);
        if (speedStat >= 5 && currentMana >= dashCost) {
            player.getPersistentData().putInt("manhwamod.current_mana", currentMana - dashCost);
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * (distanceGoal * 0.16), 0.05, look.z * (distanceGoal * 0.16));
            player.hurtMarked = true;
            SystemData.sync(player);
        }
    }

    public static void executeHeavyStrike(Player player) {
        if (!player.getPersistentData().getBoolean("manhwamod.awakened")) return;
        int strStat = SystemData.getStrength(player);
        int currentMana = player.getPersistentData().getInt("manhwamod.current_mana");
        int manaCost = (strStat >= 30) ? 15 : 8;
        if (strStat >= 5 && currentMana >= manaCost) {
            float damage = 2.0f + (strStat * 0.2f);
            EntityHitResult hitResult = getPlayerTarget(player, 3.5);
            if (hitResult != null && hitResult.getEntity() instanceof LivingEntity target) {
                target.hurt(player.damageSources().playerAttack(player), damage);
            }
            player.getPersistentData().putInt("manhwamod.current_mana", currentMana - manaCost);
            SystemData.sync(player);
        }
    }

    private static EntityHitResult getPlayerTarget(Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetPos = eyePos.add(player.getViewVector(1.0F).scale(range));
        AABB area = player.getBoundingBox().expandTowards(player.getViewVector(1.0F).scale(range)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(player.level(), player, eyePos, targetPos, area, (e) -> !e.isSpectator() && e instanceof LivingEntity);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            ServerPlayer player = (ServerPlayer) event.player;

            if (player.getPersistentData().getBoolean("manhwamod.is_system_player") && player.isSprinting()) {
                DailyQuestData.addDistance(player, 0.5);
            }

            if (player.tickCount % 20 == 0) {
                int manaStat = SystemData.getMana(player);
                int maxMana = 20 + (manaStat * 5);

                if (!player.getPersistentData().contains("manhwamod.current_mana")) {
                    player.getPersistentData().putInt("manhwamod.current_mana", maxMana);
                }

                int currentMana = player.getPersistentData().getInt("manhwamod.current_mana");
                int regenAmount = 5 + (maxMana / 20);

                if (currentMana < maxMana) {
                    player.getPersistentData().putInt("manhwamod.current_mana", Math.min(currentMana + regenAmount, maxMana));
                    SystemData.sync(player);
                }
            }
        }
    }
}