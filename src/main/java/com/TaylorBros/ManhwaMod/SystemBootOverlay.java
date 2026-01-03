package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SystemBootOverlay {
    private static int animationTicks = 0;
    private static boolean isActive = false;

    public static void startAnimation() {
        animationTicks = 80; // Animation lasts 4 seconds (20 ticks = 1s)
        isActive = true;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!isActive || animationTicks <= 0) {
            isActive = false;
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2;
        animationTicks--;

        // 1. Draw a semi-transparent "Digital Glitch" overlay
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x4400CCFF);

        // 2. Dynamic Text Phase
        String text = "§b§lAUTHENTICATING...";
        if (animationTicks < 60) text = "§b§lCONNECTING TO SYSTEM...";
        if (animationTicks < 40) text = "§b§lSYNCHRONIZING BIO-DATA...";
        if (animationTicks < 20) text = "§f§l[ ACCESS GRANTED ]";

        graphics.drawCenteredString(Minecraft.getInstance().font, text, x, y, 0xFFFFFFFF);

        // 3. Progress Bar
        int barWidth = 100;
        int progress = (int) (((80 - animationTicks) / 80.0) * barWidth);
        graphics.fill(x - 50, y + 20, x + 50, y + 25, 0xFF000000); // BG
        graphics.fill(x - 50, y + 20, x - 50 + progress, y + 25, 0xFF00CCFF); // Loading
    }
}