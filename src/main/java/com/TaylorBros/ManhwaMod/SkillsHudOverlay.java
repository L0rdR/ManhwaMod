package com.TaylorBros.ManhwaMod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SkillsHudOverlay {
    public static final IGuiOverlay HUD_SKILLS = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.player == null) return;


        int x = 10; // Left side of screen
        int y = height / 2 - 50; // Centered vertically

        guiGraphics.drawString(mc.font, "§b§lEQUIPPED ARTS", x, y - 15, 0xFFFFFF);

        for (int i = 0; i < 5; i++) {
            String recipe = mc.player.getPersistentData().getString("manhwamod.slot_" + i);
            boolean isEmpty = recipe.isEmpty();

            var font = mc.font;

            String displayName = isEmpty ? "§8[ EMPTY ]" : "§b" + SkillEngine.getSkillName(recipe);
            int rowY = y + (i * 20); // Spaced further apart for boxes

            // 1. Draw the Box (Background)
            // fill(x1, y1, x2, y2, color) - 0x80000000 is semi-transparent black
            guiGraphics.fill(x - 2, rowY - 2, x + 100, rowY + 12, 0x80000000);

            // 2. Draw an accent border (Optional: makes it look more like a "System" UI)
            int borderColor = isEmpty ? 0x40FFFFFF : 0xFF00E5FF; // White for empty, Cyan for active
            guiGraphics.fill(x - 3, rowY - 2, x - 2, rowY + 12, borderColor);

            // 3. Draw the Text
            guiGraphics.drawString(font, (i + 1) + " ", x + 2, rowY, 0xAAAAAA);
            guiGraphics.drawString(font, displayName, x + 15, rowY, 0xFFFFFF);
        }
    };
}