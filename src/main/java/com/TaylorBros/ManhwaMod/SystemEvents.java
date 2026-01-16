package com.TaylorBros.ManhwaMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import java.util.Random;

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
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        // Handled by Minecraft; no sync needed here.
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DailyQuestData.checkAndReset(player);
            // Ensure default stats exist for new players
            if (!player.getPersistentData().contains("manhwamod.points")) {
                player.getPersistentData().putInt("manhwamod.points", 5);
                player.getPersistentData().putInt("manhwamod.strength", 10);
                player.getPersistentData().putInt("manhwamod.health_stat", 10);
                player.getPersistentData().putInt(SystemData.MANA, 10);
                player.getPersistentData().putInt(SystemData.CURRENT_MANA, 100);
                player.getPersistentData().putInt("manhwamod.speed", 10);
                player.getPersistentData().putInt("manhwamod.defense", 10);
                player.getPersistentData().putBoolean("manhwamod.awakened", false);
            }
            // FORCE QUEST RESET ON NEW PLAYER / WIPE
            if (player.getPersistentData().getBoolean("manhwamod.is_system_player")) {
                // If the player has no quest date recorded, they are likely fresh/wiped
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
            if (!(player instanceof ServerPlayer sPlayer)) return;

            // 1. Existing Logic (Mana Regen, Quest Tracking)
            handleManaRegen(sPlayer);
            handleQuestTracking(sPlayer);

            // 2. NEW LOGIC: Skill Awakening Thresholds
            // We check 'manhwamod.intelligence' as used in PacketCastSkill
            int intelligence = sPlayer.getPersistentData().getInt("manhwamod.intelligence");

            // Trigger every 20 levels (Adjust as needed)
            if (intelligence > 0 && intelligence % 20 == 0) {
                String milestoneKey = "manhwamod.reward_claimed_" + intelligence;

                // If we haven't given a skill for this specific level yet...
                if (!sPlayer.getPersistentData().getBoolean(milestoneKey)) {

                    // A. Generate Random Skill from Tags
                    String newSkill = generateRandomSkill();

                    // B. Save to Player NBT List
                    int currentTotal = sPlayer.getPersistentData().getInt("manhwamod.total_unlocked");
                    sPlayer.getPersistentData().putString("manhwamod.unlocked_skill_" + currentTotal, newSkill);

                    // C. Calculate & Save Mana Cost (Scaling with Int)
                    int cost = 20 + random.nextInt(30) + (intelligence / 2);
                    sPlayer.getPersistentData().putInt("manhwamod.unlocked_cost_" + currentTotal, cost);

                    // D. Update Counts & Mark Claimed
                    sPlayer.getPersistentData().putInt("manhwamod.total_unlocked", currentTotal + 1);
                    sPlayer.getPersistentData().putBoolean(milestoneKey, true);

                    // E. Notify & Sync
                    String name = SkillEngine.getSkillName(newSkill);
                    sPlayer.displayClientMessage(Component.literal("§b[System] §fAwakening Reached! New Art: §6" + name), false);
                    SystemData.sync(sPlayer);
                }
            }
        }
    }

    // --- HELPER: Randomly combines your existing Tags ---
    private static String generateRandomSkill() {
        SkillTags.Shape[] shapes = SkillTags.Shape.values();
        SkillTags.Shape shape = shapes[random.nextInt(shapes.length)];

        SkillTags.Element[] elements = SkillTags.Element.values();
        SkillTags.Element element = elements[random.nextInt(elements.length)];

        SkillTags.Modifier[] modifiers = SkillTags.Modifier.values();
        SkillTags.Modifier modifier = modifiers[random.nextInt(modifiers.length)];

        // Returns format expected by SkillEngine: "SHAPE:ELEMENT:MODIFIER"
        return shape.name() + ":" + element.name() + ":" + modifier.name();
    }

    // ... (Refactored your existing regen/quest logic into helpers to keep TickEvent clean) ...
    private static void handleManaRegen(ServerPlayer sPlayer) {
        int manaStat = SystemData.getMana(sPlayer);
        int maxCap = manaStat * 10;
        int currentMana = SystemData.getCurrentMana(sPlayer);

        if (currentMana < maxCap) {
            double regenPerTick = 0.05 + (manaStat / 200.0);
            double buffer = sPlayer.getPersistentData().getDouble("manhwamod.mana_regen_buffer");
            double totalAddition = regenPerTick + buffer;
            int toAdd = (int) totalAddition;
            double newBuffer = totalAddition - toAdd;
            if (toAdd > 0) SystemData.saveCurrentMana(sPlayer, Math.min(maxCap, currentMana + toAdd));
            sPlayer.getPersistentData().putDouble("manhwamod.mana_regen_buffer", newBuffer);
        } else if (currentMana > maxCap) {
            SystemData.saveCurrentMana(sPlayer, maxCap);
        }
    }

    private static void handleQuestTracking(ServerPlayer player) {
        double currentDist = player.walkDist;
        double lastDist = player.getPersistentData().getDouble("manhwamod.last_dist");
        if (currentDist > lastDist) {
            DailyQuestData.addDistance(player, currentDist - lastDist);
            player.getPersistentData().putDouble("manhwamod.last_dist", currentDist);
        }
        DailyQuestData.checkAndReset(player);
    }

    // --- BALANCED DASH LOGIC ---
    public static void executeDash(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer sPlayer)) return;

        int speedStat = SystemData.getSpeed(player);
        int currentMana = SystemData.getCurrentMana(player);

        // FORMULA: Base 5 blocks. +0.5 blocks per 10 Agility. Caps at 25 blocks.
        // Needs 4000 Agility to hit max dash distance.
        double distanceGoal = Math.min(25.0, 5.0 + (speedStat * 0.05));

        int dashCost = 15; // Flat cost so high agility players can dash more often

        if (currentMana >= dashCost) {
            SystemData.saveCurrentMana(player, currentMana - dashCost);
            Vec3 look = player.getLookAngle();

            // Add velocity
            player.setDeltaMovement(look.x * (distanceGoal * 0.15), 0.2, look.z * (distanceGoal * 0.15));
            player.hurtMarked = true; // Updates client position
            SystemData.sync(sPlayer);
        }
    }

    private static void updatePlayerRank(ServerPlayer player) {
        int level = player.getPersistentData().getInt("manhwamod.level");
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
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[SYSTEM] §fRank Up: §e§l" + newRank), false);
            SystemData.sync(player);
        }

    }
    // --- BALANCED STRENGTH DAMAGE ---
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            // Only apply to physical attacks (not magic)
            if (event.getSource().isIndirect() || "magic".equals(event.getSource().getMsgId())) {
                return;
            }

            int strength = SystemData.getStrength(player);

            // FORMULA: 0.5% Damage per point
            // 100 Str = +50% Dmg.
            // 1000 Str = +500% Dmg (5x).
            // 5000 Str = +2500% Dmg (25x).
            float damageMulti = 1.0f + (strength * 0.005f);

            event.setAmount(event.getAmount() * damageMulti);
        }
    }
    // --- PERSISTENCE: Re-apply stats on Respawn ---
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Only run when respawning (not returning from End)
        if (event.isWasDeath()) {
            ServerPlayer original = (ServerPlayer) event.getOriginal();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();

            // 1. Copy our NBT Data over (Manhwa stats)
            CompoundTag oldData = original.getPersistentData();
            CompoundTag newData = newPlayer.getPersistentData();

            // List of keys to preserve
            String[] keys = {
                    SystemData.STR, SystemData.SPD, SystemData.HP, SystemData.DEF,
                    "manhwamod.intelligence", "manhwamod.stat_points",
                    "manhwamod.points", SystemData.AWAKENED, SystemData.IS_SYSTEM,
                    SystemData.BANK, SystemData.LEVEL, SystemData.XP
            };

            for (String k : keys) {
                if (oldData.contains(k)) {
                    if (k.equals(SystemData.BANK)) newData.putString(k, oldData.getString(k));
                    else if (k.equals(SystemData.AWAKENED) || k.equals(SystemData.IS_SYSTEM)) newData.putBoolean(k, oldData.getBoolean(k));
                    else newData.putInt(k, oldData.getInt(k));
                }
            }

            // 2. Re-Apply Attributes (Armor, Health, Speed)
            // We read the stats we just copied to newData
            int vit = newData.getInt(SystemData.HP);
            int agi = newData.getInt(SystemData.SPD);
            int def = newData.getInt(SystemData.DEF);

            newPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20.0 + (vit * 0.5));
            newPlayer.setHealth(newPlayer.getMaxHealth()); // Heal to full on respawn

            newPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.1 * (1.0 + (agi * 0.0005)));

            newPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(def * 0.1);
        }
    }
}