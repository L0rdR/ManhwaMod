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
            int currentMana = player.getPersistentData().getInt(SystemData.CURRENT_MANA);
            player.getPersistentData().putInt(SystemData.CURRENT_MANA, currentMana);
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
            if (player.getPersistentData().getBoolean(SystemData.IS_SYSTEM)) {
                if (player.getPersistentData().getString("manhwamod.quest_date").isEmpty()) {
                    DailyQuestData.checkAndReset(player);
                }
            }
            SystemData.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            Player player = event.player;

            if (player instanceof ServerPlayer sPlayer) {
                // 1. Mana Regeneration
                int manaStat = SystemData.getMana(sPlayer);
                int maxCap = manaStat * 10;
                int currentMana = SystemData.getCurrentMana(sPlayer);

                if (currentMana < maxCap) {
                    double regenPerTick = 0.05 + (manaStat / 200.0);
                    double buffer = sPlayer.getPersistentData().getDouble("manhwamod.mana_regen_buffer");
                    double totalAddition = regenPerTick + buffer;
                    int toAdd = (int) totalAddition;
                    if (toAdd > 0) SystemData.saveCurrentMana(sPlayer, Math.min(maxCap, currentMana + toAdd));
                    sPlayer.getPersistentData().putDouble("manhwamod.mana_regen_buffer", totalAddition - toAdd);
                } else if (currentMana > maxCap) {
                    SystemData.saveCurrentMana(sPlayer, maxCap);
                }

                // 2. SKILL GENERATION (FIXED: Threshold 20)
                int expectedSkills = manaStat / 50; // Changed from 50 to 20
                List<Integer> unlockedSkills = SystemData.getUnlockedSkills(sPlayer);

                // Use WHILE loop to catch up if multiple skills are due
                while (unlockedSkills.size() < expectedSkills) {
                    int newId = 1000 + random.nextInt(90000);
                    while (unlockedSkills.contains(newId)) newId = 1000 + random.nextInt(90000);

                    String newSkill = generateRandomSkill();
                    int cost = 20 + random.nextInt(30) + (manaStat / 2);

                    SystemData.unlockSkill(sPlayer, newId, newSkill, cost);
                    String name = SkillEngine.getSkillName(newSkill);
                    sPlayer.displayClientMessage(Component.literal("§b[System] §fAwakening Reached! New Art: §6" + name), false);

                    // Update local list to prevent infinite loop
                    unlockedSkills.add(newId);
                }

                // 3. UPDATE RANK
                updatePlayerRank(sPlayer);
            }

            // 4. Quest Tracking
            double currentDist = player.walkDist;
            double lastDist = player.getPersistentData().getDouble("manhwamod.last_dist");
            if (currentDist > lastDist) {
                DailyQuestData.addDistance(player, currentDist - lastDist);
                player.getPersistentData().putDouble("manhwamod.last_dist", currentDist);
            }
            DailyQuestData.checkAndReset(player);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            if (event.getSource().isIndirect() || "magic".equals(event.getSource().getMsgId())) return;
            int strength = SystemData.getStrength(player);
            if (strength > 10) {
                float damageMulti = 1.0f + (strength * 0.01f);
                event.setAmount(event.getAmount() * damageMulti);
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

    private static void updatePlayerRank(ServerPlayer player) {
        int level = SystemData.getLevel(player);
        String currentRank = player.getPersistentData().getString("manhwamod.rank");
        String newRank = "E";

        if (level >= 900) newRank = "SSS";
        else if (level >= 750) newRank = "SS";
        else if (level >= 600) newRank = "S";
        else if (level >= 450) newRank = "A";
        else if (level >= 300) newRank = "B";
        else if (level >= 150) newRank = "C";
        else if (level >= 50) newRank = "D";

        if (!newRank.equals(currentRank)) {
            player.getPersistentData().putString("manhwamod.rank", newRank);
            player.displayClientMessage(Component.literal("§b§l[SYSTEM] §fRank Up: §e§l" + newRank), false);
            SystemData.sync(player);
        }
    }

    private static String generateRandomSkill() {
        SkillTags.Shape[] shapes = SkillTags.Shape.values();
        SkillTags.Shape shape = shapes[random.nextInt(shapes.length)];
        SkillTags.Element[] elements = SkillTags.Element.values();
        SkillTags.Element element = elements[random.nextInt(elements.length)];
        SkillTags.Modifier[] modifiers = SkillTags.Modifier.values();
        SkillTags.Modifier modifier = modifiers[random.nextInt(modifiers.length)];
        return shape.name() + ":" + element.name() + ":" + modifier.name();
    }
}