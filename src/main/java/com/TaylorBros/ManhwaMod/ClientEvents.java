package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
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

            // STATUS SCREEN - Cascaded to use SystemData.isSystemPlayer
            while (KeyInputHandler.STATUS_KEY.consumeClick()) {
                if (SystemData.isAwakened(player)) {
                    if (SystemData.isSystemPlayer(player)) {
                        Minecraft.getInstance().setScreen(new StatusScreen());
                    } else {
                        player.sendSystemMessage(Component.literal("§c[SYSTEM] §7Authentication Denied."));
                    }
                }
            }

            // QUEST JOURNAL - Synchronized logic
            while (KeyInputHandler.QUEST_KEY.consumeClick()) {
                if (SystemData.isSystemPlayer(player)) {
                    Minecraft.getInstance().setScreen(new SystemQuestScreen());
                }
            }

            while (KeyInputHandler.DASH_KEY.consumeClick()) {
                SystemEvents.executeDash(player);
            }
        }
    }
}