package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ManhwaMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // 1. STATUS KEY (G) - Opens Phone
            while (KeyInputHandler.STATUS_KEY.consumeClick()) {
                if (SystemData.isAwakened(player)) {
                    // FIXED: Allow ALL Awakened players to open the Phone
                    Minecraft.getInstance().setScreen(new HunterPhoneScreen());
                } else {
                    player.displayClientMessage(Component.literal("§c[SYSTEM] §7Authentication Failed. You are not Awakened."), true);
                }
            }

            // 2. QUEST KEY (J)
            while (KeyInputHandler.QUEST_KEY.consumeClick()) {
                if (SystemData.isSystemPlayer(player)) {
                    // Open Phone directly since Quests are an app inside it now
                    Minecraft.getInstance().setScreen(new HunterPhoneScreen());
                } else {
                    player.displayClientMessage(Component.literal("§b§l[SYSTEM] §fOnly 'Players' can access the Quest Log."), true);
                }
            }

            // 3. DASH KEY (V)
            while (KeyInputHandler.DASH_KEY.consumeClick()) {
                SystemEvents.executeDash(player);
            }

            // 4. SKILL KEYS (Z, X, C, V, B or similar)
            if (KeyInputHandler.SKILL_1.consumeClick()) tryCastSkill(player, 0);
            if (KeyInputHandler.SKILL_2.consumeClick()) tryCastSkill(player, 1);
            if (KeyInputHandler.SKILL_3.consumeClick()) tryCastSkill(player, 2);
            if (KeyInputHandler.SKILL_4.consumeClick()) tryCastSkill(player, 3);
            if (KeyInputHandler.SKILL_5.consumeClick()) tryCastSkill(player, 4);
        }
    }

    // Helper to check cooldowns client-side before sending packet (Reduces lag)
    private static void tryCastSkill(Player player, int slotIndex) {
        int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slotIndex);
        if (skillId <= 0) return;

        long lastUse = player.getPersistentData().getLong(SystemData.LAST_USE_PREFIX + slotIndex);
        int cooldown = player.getPersistentData().getInt(SystemData.COOLDOWN_PREFIX + slotIndex);
        long timePassed = player.level().getGameTime() - lastUse;

        if (timePassed >= cooldown) {
            Messages.sendToServer(new PacketCastSkill(slotIndex));
        } else {
            // Optional: Play a "buzzer" sound if on cooldown?
        }
    }

    @SubscribeEvent
    public static void registerGuiOverlaysEvent(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_hud", SkillsHudOverlay.HUD_SKILLS);
    }
}