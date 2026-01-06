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

            while (KeyInputHandler.STATUS_KEY.consumeClick()) {
                if (SystemData.isAwakened(player)) {
                    if (SystemData.isSystemPlayer(player)) {
                        Minecraft.getInstance().setScreen(new StatusScreen());
                    } else {
                        player.sendSystemMessage(Component.literal("§c[SYSTEM] §7Error: Use an Evaluation Crystal."));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§c[SYSTEM] §7Authentication Failed."));
                }
            }

            while (KeyInputHandler.QUEST_KEY.consumeClick()) {
                if (SystemData.isSystemPlayer(player)) {
                    Minecraft.getInstance().setScreen(new SystemQuestScreen());
                } else {
                    player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fOnly 'Players' can access the Daily Log."));
                }
            }

            while (KeyInputHandler.DASH_KEY.consumeClick()) {
                SystemEvents.executeDash(player);
            }
        }
    }
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_hud", SkillsHudOverlay.HUD_SKILLS);
    }
}