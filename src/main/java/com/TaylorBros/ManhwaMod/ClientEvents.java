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
                    boolean isSystemPlayer = player.getPersistentData().getBoolean("manhwamod.is_system_player");
                    if (isSystemPlayer) {
                        Minecraft.getInstance().setScreen(new StatusScreen());
                    } else {
                        player.sendSystemMessage(Component.literal("§c[SYSTEM] §7Error: Use an Evaluation Crystal."));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§c[SYSTEM] §7Authentication Failed."));
                }
            }

            // 2. QUEST JOURNAL (J KEY) - NEW LOGIC
            while (KeyInputHandler.QUEST_KEY.consumeClick()) {
                boolean isSystemPlayer = player.getPersistentData().getBoolean("manhwamod.is_system_player");
                if (isSystemPlayer) {
                    Minecraft.getInstance().setScreen(new SystemQuestScreen());
                } else {
                    player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fOnly 'Players' can access the Daily Log."));
                }
            }

            // 3. SKILLS (V KEY)
            while (KeyInputHandler.DASH_KEY.consumeClick()) {
                SystemEvents.executeDash(player);
            }
        }
    }
}