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
        if (mc.player == null) return;

        var font = mc.font;
        int x = 10;
        int y = height / 2 - 50;

        for (int i = 0; i < 5; i++) {
            // We pass 'mc.player' because SystemData needs it to access NBT
            String recipe = SystemData.getSkillRecipe(mc.player, i);
            boolean isEmpty = recipe.isEmpty() || recipe.equals("0");

            String displayName = recipe.isEmpty() ? "§7" + (i + 1) + ": §8[ Empty ]" : "§f" + (i + 1) + ": §b" + SkillEngine.getSkillName(recipe);
            int rowY = y + (i * 20); // Spaced for boxes

            // 1. Draw the Background Box (Semi-transparent black)
            guiGraphics.fill(x - 2, rowY - 2, x + 100, rowY + 12, 0x80000000);

            // 2. Draw the Accent Border (Cyan for active, gray for empty)
            int borderColor = isEmpty ? 0x40FFFFFF : 0xFF00E5FF;
            guiGraphics.fill(x - 3, rowY - 2, x - 2, rowY + 12, borderColor);

            // 3. Draw the Skill Name
            guiGraphics.drawString(font, (i + 1) + " ", x + 2, rowY, 0xAAAAAA);        }
    };
}