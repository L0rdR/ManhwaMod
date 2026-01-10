package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SkillsHudOverlay {
    public static final IGuiOverlay HUD_SKILLS = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var font = mc.font;
        int x = 10;
        int y = height / 2 - 50;

        for (int i = 0; i < 5; i++) {
            String recipe = SystemData.getSkillRecipe(mc.player, i);
            boolean isEmpty = recipe == null || recipe.isEmpty() || recipe.equals("0");

            String displayName = isEmpty ? "§8" + (i + 1) + ": [ EMPTY ]" : "§b" + (i + 1) + ": " + SkillEngine.getSkillName(recipe);

            // Simple text display to ensure it works first
            guiGraphics.drawString(font, displayName, x, y + (i * 15), 0xFFFFFF);
        }
    };
}