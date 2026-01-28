package com.TaylorBros.ManhwaMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraft.network.chat.Component;
import java.util.Random;
import java.util.List;

@Mod.EventBusSubscriber(modid = ManhwaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SystemEvents {

    private static final Random random = new Random();

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().putInt(SystemData.CURRENT_MANA, player.getPersistentData().getInt(SystemData.CURRENT_MANA));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            DailyQuestData.addKill(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DailyQuestData.checkAndReset(player);
            if (!player.getPersistentData().contains(SystemData.POINTS)) {
                player.getPersistentData().putInt(SystemData.POINTS, 5);
                player.getPersistentData().putInt(SystemData.STR, 10);
                player.getPersistentData().putInt(SystemData.HP, 10);
                player.getPersistentData().putInt(SystemData.MANA, 10);
                player.getPersistentData().putInt(SystemData.CURRENT_MANA, 100);
                player.getPersistentData().putInt(SystemData.SPD, 10);
                player.getPersistentData().putInt(SystemData.DEF, 10);
                player.getPersistentData().putBoolean(SystemData.AWAKENED, false);
            }
            // ANTI-FREEZE: Do not sync immediately. Flag for delayed sync.
            player.getPersistentData().putBoolean("manhwamod.needs_initial_sync", true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            if (event.player instanceof ServerPlayer sPlayer) {

                // --- ANTI-FREEZE LOGIC ---
                // Waits 1 second (20 ticks) after joining before sending the heavy data packet
                if (sPlayer.getPersistentData().getBoolean("manhwamod.needs_initial_sync")) {
                    if (sPlayer.tickCount > 20) {
                        SystemData.sync(sPlayer);
                        sPlayer.getPersistentData().putBoolean("manhwamod.needs_initial_sync", false);
                    }
                    return; // Skip other logic until synced
                }

                // 0. AUTO-AFFINITY ASSIGNER
                if (SystemData.isAwakened(sPlayer) && SystemData.getAffinity(sPlayer) == Affinity.NONE) {
                    Affinity[] values = Affinity.values();
                    Affinity rolled = values[1 + random.nextInt(values.length - 1)];
                    SystemData.setAffinity(sPlayer, rolled);
                    sPlayer.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fYour soul has resonated with: " + rolled.color + rolled.name()));
                }

                // 1. Mana Regen (Throttled Sync)
                int manaStat = SystemData.getMana(sPlayer);
                int maxCap = manaStat * 10;
                int currentMana = SystemData.getCurrentMana(sPlayer);
                boolean dirty = false;

                if (currentMana < maxCap) {
                    double regenPerTick = 0.05 + (manaStat / 200.0);
                    double buffer = sPlayer.getPersistentData().getDouble("manhwamod.mana_regen_buffer");
                    double totalAddition = regenPerTick + buffer;
                    int toAdd = (int) totalAddition;
                    if (toAdd > 0) {
                        SystemData.saveCurrentMana(sPlayer, Math.min(maxCap, currentMana + toAdd));
                        sPlayer.getPersistentData().putDouble("manhwamod.mana_regen_buffer", totalAddition - toAdd);
                        // Do NOT sync here. Just save.
                    } else {
                        sPlayer.getPersistentData().putDouble("manhwamod.mana_regen_buffer", totalAddition);
                    }
                }

                // 2. Skill Milestone Logic
                int expectedSkills = manaStat / 50;
                List<Integer> unlockedSkills = SystemData.getUnlockedSkills(sPlayer);

                while (unlockedSkills.size() < expectedSkills) {
                    int newId = 1000 + random.nextInt(90000);
                    String recipe = generateRandomSkill(sPlayer);
                    int cost = 20 + random.nextInt(30) + (manaStat / 2);

                    String[] parts = recipe.split(":");
                    String elementWord = SkillNamingEngine.getElementName(parts[1]);
                    String shapeWord = SkillNamingEngine.getShapeName(parts[0]);
                    String modWord = SkillNamingEngine.getModifierName(parts[2]);
                    String coolName = elementWord + " " + shapeWord + " " + modWord;

                    SystemData.unlockSkill(sPlayer, newId, recipe + "|" + coolName.trim(), cost);
                    sPlayer.displayClientMessage(Component.literal("§b[System] §fNew Art: §6§l" + coolName.trim()), false);
                    unlockedSkills.add(newId);
                    dirty = true;
                }

                // 3. Update Rank
                if (updatePlayerRank(sPlayer)) dirty = true;

                // 4. Quest Tracking
                double currentDist = sPlayer.walkDist;
                double lastDist = sPlayer.getPersistentData().getDouble("manhwamod.last_dist");
                if (currentDist > lastDist) {
                    DailyQuestData.addDistance(sPlayer, currentDist - lastDist);
                    sPlayer.getPersistentData().putDouble("manhwamod.last_dist", currentDist);
                }
                DailyQuestData.checkAndReset(sPlayer);

                // --- GLOBAL SYNC THROTTLE ---
                // Only allows 1 packet per second maximum
                if (dirty) {
                    long lastSync = sPlayer.getPersistentData().getLong("manhwamod.last_sync_time");
                    if (sPlayer.level().getGameTime() - lastSync > 20) {
                        SystemData.sync(sPlayer);
                        sPlayer.getPersistentData().putLong("manhwamod.last_sync_time", sPlayer.level().getGameTime());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Attack Boost
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            if (!event.getSource().isIndirect() && !"magic".equals(event.getSource().getMsgId())) {
                int strength = SystemData.getStrength(player);
                if (strength > 10) {
                    float damageMulti = 1.0f + (strength * 0.01f);
                    event.setAmount(event.getAmount() * damageMulti);
                }
            }
        }

        // Elemental Defense (Victim)
        if (event.getEntity() instanceof ServerPlayer victim) {
            SkillTags.Element moveElement = SkillEngine.currentSkillElement;
            if (moveElement != SkillTags.Element.NONE) {
                Affinity victimAff = SystemData.getAffinity(victim);
                try {
                    Affinity moveAff = Affinity.valueOf(moveElement.name());
                    if (victimAff == moveAff) {
                        event.setAmount(event.getAmount() * 0.90f);
                        victim.displayClientMessage(Component.literal("§b🛡 ELEMENTAL RESISTANCE"), true);
                    }
                    if (victimAff.getWeaknesses().contains(moveAff)) {
                        event.setAmount(event.getAmount() * 1.10f);
                        victim.displayClientMessage(Component.literal("§c⚠ ELEMENTAL WEAKNESS"), true);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public static void executeDash(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer sPlayer)) return;
        int speedStat = SystemData.getSpeed(player);
        int currentMana = SystemData.getCurrentMana(player);
        double distanceGoal = Math.min(30.0, 5.0 + (speedStat * 0.2));
        int dashCost = 15;

        if (currentMana >= dashCost) {
            SystemData.saveCurrentMana(player, currentMana - dashCost);
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * (distanceGoal * 0.15), 0.2, look.z * (distanceGoal * 0.15));
            player.hurtMarked = true;
            SystemData.sync(sPlayer);
        }
    }

    private static boolean updatePlayerRank(ServerPlayer player) {
        int level = SystemData.getLevel(player);
        String currentRank = player.getPersistentData().getString("manhwamod.rank");
        String newRank = (level >= 900) ? "SSS" : (level >= 750) ? "SS" : (level >= 600) ? "S" : (level >= 450) ? "A" : (level >= 300) ? "B" : (level >= 150) ? "C" : (level >= 50) ? "D" : "E";

        if (!newRank.equals(currentRank)) {
            player.getPersistentData().putString("manhwamod.rank", newRank);
            player.displayClientMessage(Component.literal("§b§l[SYSTEM] §fRank Up: §e§l" + newRank), false);
            return true; // Return true to signal a sync is needed
        }
        return false;
    }

    private static String generateRandomSkill(ServerPlayer player) {
        SkillTags.Shape shape = SkillTags.Shape.values()[random.nextInt(SkillTags.Shape.values().length)];
        SkillTags.Modifier modifier = SkillTags.Modifier.values()[random.nextInt(SkillTags.Modifier.values().length)];
        SkillTags.Element element;
        Affinity playerAff = SystemData.getAffinity(player);

        if (random.nextFloat() < 0.65f && playerAff != Affinity.NONE) {
            try { element = SkillTags.Element.valueOf(playerAff.name()); }
            catch (Exception e) { element = SkillTags.Element.values()[random.nextInt(SkillTags.Element.values().length)]; }
        } else {
            element = SkillTags.Element.values()[random.nextInt(SkillTags.Element.values().length)];
        }
        return shape.name() + ":" + element.name() + ":" + modifier.name();
    }
}