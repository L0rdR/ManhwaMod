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
        int y = height / 2 - 40; // Centered left

        // 1. Draw Container Background (Panel for all 5 skills)
        // x, y, width, height, color (0x66000000 is semi-transparent black)
        guiGraphics.fill(x - 5, y - 5, x + 130, y + 105, 0x66000000);
        // 2. Draw Cyan Decoration Line on Left
        guiGraphics.fill(x - 5, y - 5, x - 3, y + 105, 0xFF00AAFF);

        for (int i = 0; i < 5; i++) {
            int skillId = mc.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + i);
            String recipe = SystemData.getSkillRecipe(mc.player, i);
            boolean isEmpty = skillId <= 0 || recipe.isEmpty();
            int rowY = y + (i * 20);

            if (!isEmpty) {
                long lastUse = mc.player.getPersistentData().getLong(SystemData.LAST_USE_PREFIX + i);
                int duration = mc.player.getPersistentData().getInt(SystemData.COOLDOWN_PREFIX + i);
                long timeLeft = duration - (mc.level.getGameTime() - lastUse);

                // Cooldown Bar (Yellow)
                if (timeLeft > 0 && duration > 0) {
                    float pct = (float) timeLeft / duration;
                    int barWidth = (int) (pct * 125);
                    guiGraphics.fill(x, rowY + 12, x + barWidth, rowY + 13, 0xFFFFD700);
                    guiGraphics.drawString(font, "§7" + (i + 1) + ". §cCooldown...", x, rowY, 0xFFFFFF);
                } else {
                    // Ready
                    String name = SkillEngine.getSkillName(recipe);
                    guiGraphics.drawString(font, "§b" + (i + 1) + ". §f" + name, x, rowY, 0xFFFFFF);
                }
            } else {
                guiGraphics.drawString(font, "§8" + (i + 1) + ". [ --- ]", x, rowY, 0xFFFFFF);
            }
        }
    };
}