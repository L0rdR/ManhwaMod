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
        // Only render on top of the experience bar
        if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            // RULE 2: Logic - Ensure player is awakened before drawing
            if (player != null && SystemData.isAwakened(player)) {
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();
                GuiGraphics graphics = event.getGuiGraphics();

                // RULE 1: Use Centralized Constants
                int currentMana = player.getPersistentData().getInt(SystemData.CURRENT_MANA);
                int maxMana = player.getPersistentData().getInt(SystemData.MANA);

                // RULE 3: Fail-safe - Don't divide by zero
                if (maxMana <= 0) maxMana = 1;

                // 2. Calculate Bar Width (Max width is 100 pixels)
                float ratio = (float) currentMana / maxMana;
                int barWidth = (int) (ratio * 100);

                // 3. Render Logic
                int x = width / 2 - 50;
                int y = height - 50;

                // Draw Background (Dark)
                graphics.fill(x - 1, y - 1, x + 101, y + 6, 0xFF000000);
                // Draw Mana Bar (Vibrant Blue for Manhwa Style)
                graphics.fill(x, y, x + barWidth, y + 5, 0xFF00FBFF);

                // 4. Text Label
                String text = "MANA: " + currentMana + " / " + maxMana;
                graphics.drawString(mc.font, text, x + 50 - (mc.font.width(text) / 2), y - 10, 0xFF00FBFF, true);
            }
        }
    }
}