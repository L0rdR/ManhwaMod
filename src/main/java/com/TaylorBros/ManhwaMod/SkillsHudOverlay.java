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
            int rowY = y + (i * 20);

            // 1. Translucent Background (0x44 is the transparency level)
            guiGraphics.fill(x - 2, rowY - 2, x + 120, rowY + 14, 0x44000000);

            if (!isEmpty) {
                long lastUse = mc.player.getPersistentData().getLong(SystemData.LAST_USE_PREFIX + i);
                int cooldownMax = mc.player.getPersistentData().getInt(SystemData.COOLDOWN_PREFIX + i);
                long elapsed = mc.level.getGameTime() - lastUse;

                // 2. Flash Effect (White highlight for 3 ticks)
                if (elapsed >= 0 && elapsed < 3) {
                    guiGraphics.fill(x - 2, rowY - 2, x + 120, rowY + 14, 0x88FFFFFF);
                }

                // 3. Cooldown Bar
                if (elapsed < cooldownMax) {
                    float pct = 1.0f - ((float) elapsed / cooldownMax);
                    guiGraphics.fill(x - 2, rowY + 12, x - 2 + (int)(pct * 122), rowY + 14, 0xFFFFD700);
                }

                String displayName = "§6" + (i + 1) + ": " + SkillEngine.getSkillName(recipe);
                guiGraphics.drawString(font, displayName, x, rowY, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, "§8" + (i + 1) + ": [ EMPTY ]", x, rowY, 0xFFFFFF);
            }
        }
    };
}