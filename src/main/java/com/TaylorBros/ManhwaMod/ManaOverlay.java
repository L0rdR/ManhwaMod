package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ManaOverlay {
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player != null && SystemData.isAwakened(player)) {
                GuiGraphics graphics = event.getGuiGraphics();
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();

                int currentMana = SystemData.getCurrentMana(player);
                int maxMana = SystemData.getMana(player);
                if (maxMana <= 0) maxMana = 1;

                float ratio = (float) currentMana / maxMana;
                int barWidth = (int) (ratio * 100);

                int x = width / 2 - 50;
                int y = height - 50;

                graphics.fill(x - 1, y - 1, x + 101, y + 6, 0xFF000000);
                graphics.fill(x, y, x + barWidth, y + 5, 0xFF00AAFF);

                String text = currentMana + " / " + maxMana;
                graphics.drawString(mc.font, text, x + 50 - (mc.font.width(text) / 2), y - 10, 0xFFFFFFFF);
            }
        }
    }
}