package com.TaylorBros.ManhwaMod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ManaOverlay {
    // You'll need a simple blue square texture at this path later
    private static final ResourceLocation MANA_BAR = new ResourceLocation(ManhwaMod.MODID, "textures/gui/mana_bar.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        // We only want to draw on top of the experience bar
        if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player != null && SystemData.isAwakened(player)) {
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();
                GuiGraphics graphics = event.getGuiGraphics();

                // 1. Get Mana Data
                int manaPoints = SystemData.getMana(player);
                int currentMana = player.getPersistentData().getInt("manhwamod.current_mana");
                int maxMana = 20 + (manaPoints * 5);

                // 2. Calculate Bar Width (Max width is 100 pixels)
                float ratio = (float) currentMana / maxMana;
                int barWidth = (int) (ratio * 100);

                // 3. Render Logic
                int x = width / 2 - 50; // Center it
                int y = height - 50;    // Place it above the hotbar

                // Draw Background (Dark)
                graphics.fill(x - 1, y - 1, x + 101, y + 6, 0xFF000000);
                // Draw Mana Bar (Blue)
                graphics.fill(x, y, x + barWidth, y + 5, 0xFF00AAFF);

                // 4. Text Label
                String text = currentMana + " / " + maxMana;
                graphics.drawString(mc.font, text, x + 50 - (mc.font.width(text) / 2), y - 10, 0xFFFFFFFF);
            }
        }
    }
}