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

            // 1. STATUS SCREEN (G KEY)
            while (KeyInputHandler.STATUS_KEY.consumeClick()) {
                if (SystemData.isAwakened(player)) {
                    // FIXED: Now opens the correct screen for Awakened Players
                    Minecraft.getInstance().setScreen(new AwakenedStatusScreen());
                } else {
                    player.sendSystemMessage(Component.literal("§c[SYSTEM] §7Authentication Failed."));
                }
            }

            // 2. QUEST JOURNAL (J KEY)
            while (KeyInputHandler.QUEST_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new SystemQuestScreen());
            }

            // 3. DASH (V KEY)
            while (KeyInputHandler.DASH_KEY.consumeClick()) {
                SystemEvents.executeDash(player);
            }
        }
    }
}