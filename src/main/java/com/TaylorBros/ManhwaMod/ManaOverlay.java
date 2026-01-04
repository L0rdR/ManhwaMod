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
        // Only render on the post-experience bar layer
        if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player != null && SystemData.isAwakened(player)) {
                GuiGraphics graphics = event.getGuiGraphics();
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();

                // 1. Get the current mana (e.g., 100)
                int currentMana = SystemData.getCurrentMana(player);

                // 2. Get the Mana Stat (e.g., 10)
                int manaStat = SystemData.getMana(player);

                // 3. DEFINITIVE BUSINESS LOGIC: 1 Stat = 10 Max Mana Pool
                int maxPool = manaStat * 10;
                if (maxPool <= 0) maxPool = 1;

                // 4. Calculate Bar Width (0% to 100%)
                // This prevents the "overflow" look by scaling to the Pool, not the Stat.
                float ratio = Math.min(1.0f, (float) currentMana / maxPool);
                int barWidth = (int) (ratio * 100);

                // Position the bar at the bottom center
                int x = width / 2 - 50;
                int y = height - 50;

                // Render Background (Black)
                graphics.fill(x - 1, y - 1, x + 101, y + 6, 0xFF000000);
                // Render Progress (Cyan/Blue)
                graphics.fill(x, y, x + barWidth, y + 5, 0xFF00AAFF);

                // 5. Update the Text display to show "100 / 100" instead of "100 / 10"
                String text = currentMana + " / " + maxPool;
                graphics.drawString(mc.font, text, x + 50 - (mc.font.width(text) / 2), y - 10, 0xFFFFFFFF);
            }
        }
    }
}